package com.orderflow.autoresponder.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.orderflow.autoresponder.data.local.OrderFlowDatabase
import com.orderflow.autoresponder.data.local.dao.CustomerDao
import com.orderflow.autoresponder.data.local.dao.MessageLogDao
import com.orderflow.autoresponder.data.local.dao.ProcessedNotificationDao
import com.orderflow.autoresponder.data.local.dao.RuleDao
import com.orderflow.autoresponder.data.local.dao.RuleMessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create rule_messages table
            db.execSQL("CREATE TABLE IF NOT EXISTS `rule_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ruleId` INTEGER NOT NULL, `message` TEXT NOT NULL, `position` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, FOREIGN KEY(`ruleId`) REFERENCES `auto_reply_rules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_rule_messages_ruleId` ON `rule_messages` (`ruleId`)")

            // 2. Add initialDelaySeconds to auto_reply_rules
            db.execSQL("ALTER TABLE `auto_reply_rules` ADD COLUMN `initialDelaySeconds` INTEGER NOT NULL DEFAULT 0")

            // 3. Migrate data: Split replyMessagesJson into multiple messages
            val cursor = db.query("SELECT id, replyMessagesJson FROM auto_reply_rules")
            if (cursor.moveToFirst()) {
                do {
                    val ruleId = cursor.getLong(0)
                    val json = cursor.getString(1) ?: ""
                    
                    // Simple split logic compatible with previous implementation
                    val messages = json.split("||", "\n")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    
                    messages.forEachIndexed { index, msg ->
                        db.execSQL(
                            "INSERT INTO rule_messages (ruleId, message, position, isEnabled) VALUES (?, ?, ?, ?)",
                            arrayOf(ruleId, msg, index, 1)
                        )
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OrderFlowDatabase {
        return Room.databaseBuilder(
            context,
            OrderFlowDatabase::class.java,
            "orderflow_autoresponder.db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideRuleDao(database: OrderFlowDatabase): RuleDao = database.ruleDao()

    @Provides
    fun provideMessageLogDao(database: OrderFlowDatabase): MessageLogDao = database.messageLogDao()

    @Provides
    fun provideCustomerDao(database: OrderFlowDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideProcessedNotificationDao(database: OrderFlowDatabase): ProcessedNotificationDao = 
        database.processedNotificationDao()

    @Provides
    fun provideRuleMessageDao(database: OrderFlowDatabase): RuleMessageDao = 
        database.ruleMessageDao()
}
