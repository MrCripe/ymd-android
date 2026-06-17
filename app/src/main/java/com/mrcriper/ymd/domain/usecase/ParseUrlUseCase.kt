package com.mrcriper.ymd.domain.usecase

import com.mrcriper.ymd.data.remote.api.YandexEntity
import com.mrcriper.ymd.data.remote.api.detectYandexEntityType
import javax.inject.Inject

class ParseUrlUseCase @Inject constructor() {
    operator fun invoke(input: String): YandexEntity? = detectYandexEntityType(input)
}
