package com.github.brainhu.kirogui.model

import java.time.Instant

data class SteeringRuleFile(
    val name: String,
    val path: String,
    val lastModified: Instant
)
