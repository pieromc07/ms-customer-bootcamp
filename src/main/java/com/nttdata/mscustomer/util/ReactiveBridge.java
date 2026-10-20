package com.nttdata.mscustomer.util;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Converts between Reactor types used by Spring Data and WebFlux and the RxJava types used in the service layer.
 */
public final class ReactiveBridge {

    private ReactiveBridge() {
    }

    public static <T> Single<T> toSingle(Mono<T> mono) {
        return Flowable.fromPublisher(mono).firstOrError();
    }

    public static <T> Maybe<T> toMaybe(Mono<T> mono) {
        return Flowable.fromPublisher(mono).firstElement();
    }

    public static <T> Flowable<T> toFlowable(Flux<T> flux) {
        return Flowable.fromPublisher(flux);
    }

    public static <T> Mono<T> toMono(Single<T> single) {
        return Mono.from(single.toFlowable());
    }

    public static <T> Flux<T> toFlux(Flowable<T> flowable) {
        return Flux.from(flowable);
    }

    public static Mono<Void> toVoid(Completable completable) {
        return Mono.from(completable.toFlowable()).then();
    }
}
