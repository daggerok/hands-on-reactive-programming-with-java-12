package daggerok._12;

import com.jayway.awaitility.Duration;
import lombok.Cleanup;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

class HotPublisherTest {

  // @Test
  void bad_test() {
    // given:
    @Cleanup var publisher = new SubmissionPublisher<Integer>();
    var atomicInteger = new AtomicInteger();
    var subscriber = new Flow.Subscriber<Integer>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("on subscribe: %s%n", subscription);
        subscription.request(Integer.MAX_VALUE); // producer is unbounded
      }

      @Override
      public void onNext(Integer item) {
        System.out.printf("on next: %s%n", item);
        atomicInteger.incrementAndGet();
      }

      @Override
      public void onError(Throwable throwable) {
        System.out.printf("error: %s%n", throwable.getLocalizedMessage());
      }

      @Override
      public void onComplete() {
        System.out.printf("done!%n");
      }
    };
    Stream<Integer> infiniteProducer = Stream.iterate(0, integer -> integer + 2);

    // when:
    publisher.subscribe(subscriber);
    infiniteProducer.forEach(publisher::submit);

    // then:
    await().atMost(Duration.TWO_SECONDS)
           .until(() -> assertThat(atomicInteger.get()).isEqualTo(2));
  }

  @Test
  void test_with_backpressure() {
    // given:
    @Cleanup var publisher = new SubmissionPublisher<Integer>();
    var atomicInteger = new AtomicInteger();
    var subscriber = new Flow.Subscriber<Integer>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("on subscribe: %s%n", subscription);
        subscription.request(1); // <-- cold publisher: now subscription is not unbounded...
      }

      @Override
      public void onNext(Integer item) {
        System.out.printf("on next: %s%n", item);
        atomicInteger.incrementAndGet();
      }

      @Override
      public void onError(Throwable throwable) {
        System.out.printf("error: %s%n", throwable.getLocalizedMessage());
      }

      @Override
      public void onComplete() {
        System.out.printf("done!%n");
      }
    };
    Stream<Integer> infiniteProducer = Stream.iterate(0, integer -> integer + 2)
                                             .limit(10);

    // when:
    publisher.subscribe(subscriber);
    infiniteProducer.forEach(publisher::submit);

    // then:
    await().atMost(Duration.TWO_SECONDS)
           .until(() -> assertThat(atomicInteger.get()).isEqualTo(1)); // <-- see here
  }
}
