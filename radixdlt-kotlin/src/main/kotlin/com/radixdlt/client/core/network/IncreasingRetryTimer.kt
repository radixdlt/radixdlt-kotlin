package com.radixdlt.client.core.network

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import io.reactivex.rxkotlin.zipWith
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class IncreasingRetryTimer : Function<Observable<Throwable>, ObservableSource<Long>> {

    private val LOGGER = LoggerFactory.getLogger(IncreasingRetryTimer::class.java)

    override fun apply(attempts: Observable<Throwable>): ObservableSource<Long> {
        return attempts.zipWith(Observable.range(1, 300)) { _, i -> i }
            .map { i -> Math.min(i * i, 100) }
            .doOnNext { i -> LOGGER.info("Connection lost. Retrying in $i seconds...") }
            .flatMap { i -> Observable.timer(i.toLong(), TimeUnit.SECONDS) }
    }
}
