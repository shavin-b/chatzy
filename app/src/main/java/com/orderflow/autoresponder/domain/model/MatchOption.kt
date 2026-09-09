package com.orderflow.autoresponder.domain.model

enum class MatchOption {
    EXACT,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    REGEX,
    ALL,
    AWAY
}
