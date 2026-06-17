package com.mrcriper.ymd.domain.usecase

import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.data.remote.api.detectYandexEntityType

class ParseUrlUseCase {
    operator fun invoke(input: String): YandexEntity? = detectYandexEntityType(input)
}
