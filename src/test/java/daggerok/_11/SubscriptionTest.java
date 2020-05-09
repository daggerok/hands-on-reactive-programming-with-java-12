package daggerok._11;

import com.jayway.awaitility.Duration;
import lombok.Cleanup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionTest {

  @Test
  void test() {
    // given:
    @Cleanup var publisher = new SubmissionPublisher<String>();
    var atomicInteger = new AtomicInteger();
    var subscriber = new Flow.Subscriber<String>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        System.out.printf("on subscribe: %s%n", subscription);
        subscription.request(Integer.MAX_VALUE); // backpressure should comes here...
      }

      @Override
      public void onNext(String item) {
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

    // when:
    publisher.subscribe(subscriber);
    List.of("ololo", "trololo")
        .forEach(publisher::submit);

    // then:
    await().atMost(Duration.TWO_SECONDS)
           .until(() -> assertThat(atomicInteger.get()).isEqualTo(2));
  }
}
