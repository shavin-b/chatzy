"""Read-only contract verification for branding upgrades; no Android/Firebase writes."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / 'app/src/main'
NS = '{http://schemas.android.com/apk/res/android}'

def verify():
    gradle = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
    assert 'namespace = "com.orderflow.autoresponder"' in gradle
    assert 'applicationId = "com.orderflow.autoresponder"' in gradle
    database = (ANDROID / 'java/com/orderflow/autoresponder/data/local/OrderFlowDatabase.kt').read_text()
    assert 'version = 6' in database
    module = (ANDROID / 'java/com/orderflow/autoresponder/di/DatabaseModule.kt').read_text()
    assert '"orderflow_autoresponder.db"' in module
    assert 'fallbackToDestructiveMigration' not in module
    assert '.addMigrations(MIGRATION_4_5)' in module
    storage = (ANDROID / 'java/com/orderflow/autoresponder/core/security/SecureStorage.kt').read_text()
    assert '"orderflow_secure_prefs"' in storage
    device = (ANDROID / 'java/com/orderflow/autoresponder/device/service/DeviceManager.kt').read_text()
    assert '"OF-${androidId.take(16)}"' in device
    manifest = ET.parse(ANDROID / 'AndroidManifest.xml').getroot()
    application = manifest.find('application')
    assert application.get(NS+'icon') == '@mipmap/ic_launcher'
    assert application.get(NS+'roundIcon') == '@mipmap/ic_launcher_round'
    listener = next(s for s in application.findall('service') if s.get(NS+'name') == '.service.WhatsAppNotificationListener')
    assert listener.get(NS+'permission') == 'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE'
    strings = ET.parse(ANDROID / 'res/values/strings.xml').getroot()
    assert next(s.text for s in strings if s.get('name') == 'app_name') == 'Chatzy'
    for xml in (ANDROID / 'res').rglob('*.xml'):
        ET.parse(xml)
    routes = (ROOT / 'admin_web/src/App.tsx').read_text()
    expected = {'/login','/','/devices','/devices/:deviceId','/subscriptions','/activity','/users','/settings','*'}
    assert set(re.findall(r'path="([^"]+)"',routes)) == expected
    html = (ROOT / 'admin_web/index.html').read_text()
    assert 'Chatzy Admin Console | SHAVIN Solutions' in html and 'href="/favicon.svg"' in html
    worker = (ROOT / 'admin_web/cloudflare-worker/src/index.ts').read_text()
    assert 'order-flow-34826' not in worker
    config = json.loads((ROOT / 'admin_web/firebase.json').read_text())
    assert config['firestore']['rules'] == 'firestore.rules'  # staged target must stay isolated
    baseline = ROOT / '.artifacts/chatzy-baseline/files'
    compared = 0
    if baseline.exists():
        protected = ['app/src/test', 'app/src/main/java/com/orderflow/autoresponder/domain',
                     'app/src/main/java/com/orderflow/autoresponder/data/local',
                     'app/src/main/java/com/orderflow/autoresponder/core/security',
                     'app/src/main/java/com/orderflow/autoresponder/core/util/ConversationLockManager.kt',
                     'app/src/main/java/com/orderflow/autoresponder/core/util/ActiveSequenceTracker.kt',
                     'app/src/main/java/com/orderflow/autoresponder/service/parser',
                     'app/src/main/java/com/orderflow/autoresponder/service/BootReceiver.kt',
                     'app/src/main/java/com/orderflow/autoresponder/service/NotificationServiceWatchdog.kt']
        for prefix in protected:
            item = baseline / prefix
            files = item.rglob('*') if item.is_dir() else [item]
            for old in files:
                if not old.is_file(): continue
                relative = old.relative_to(baseline)
                assert old.read_bytes() == (ROOT / relative).read_bytes(), f'Protected contract changed: {relative}'
                compared += 1
    print(f'Compatibility contracts passed; {compared} protected source/test files match the initial source checkpoint.')
    print('Runtime upgrade, historical Room migration and real Firebase/FCM verification remain manual release gates.')

if __name__ == '__main__':
    verify()
