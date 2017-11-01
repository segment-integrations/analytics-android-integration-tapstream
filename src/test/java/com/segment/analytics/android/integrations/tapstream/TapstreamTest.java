package com.segment.analytics.android.integrations.tapstream;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.AliasPayloadBuilder;
import com.segment.analytics.test.GroupPayloadBuilder;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Tapstream;
import org.assertj.core.data.MapEntry;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.android.integrations.tapstream.TapstreamTest.EventMatcher.eventEq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.robolectric.annotation.Config.NONE;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(constants = BuildConfig.class, sdk = 18, manifest = NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Tapstream.class) //
public class TapstreamTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Tapstream tapstream;
  Config config;
  @Mock Application context;
  @Mock Analytics analytics;
  TapstreamIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Tapstream.class);

    PowerMockito.when(Tapstream.getInstance()).thenReturn(tapstream);
    when(analytics.getApplication()).thenReturn(context);
    when(analytics.logger("Tapstream")).thenReturn(Logger.with(VERBOSE));

    integration = new TapstreamIntegration(analytics, new ValueMap() //
        .putValue("accountName", "foo")
        .putValue("sdkSecret", "bar")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true));

    // Reset mocks.
    PowerMockito.mockStatic(Tapstream.class);

    config = new Config();
    integration.config = config;
  }

  @Test public void initialize() throws IllegalStateException {
    TapstreamIntegration integration = new TapstreamIntegration(analytics, new ValueMap() //
        .putValue("accountName", "foo")
        .putValue("sdkSecret", "bar")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true));

    verifyStatic();
    Tapstream.create(eq(context), eq("foo"), eq("bar"), any(Config.class));

    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(tapstream).fireEvent(eventEq("foo"));
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screen() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenAllPages() {
    integration.trackAllPages = true;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenNamedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = false;
    integration.trackNamedPages = true;

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void screenCategorizedPages() {
    integration.trackAllPages = false;
    integration.trackCategorizedPages = true;
    integration.trackNamedPages = false;

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(tapstream).fireEvent(eventEq("viewed foo screen"));
    verifyNoMoreTapstreamInteractions();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void flush() {
    integration.flush();
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void identify() {
    Traits traits = new Traits().putValue("foo", "bar").putValue("baz", "qux");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    assertThat(config.globalEventParams).hasSize(2).contains(MapEntry.entry("foo", "bar")) //
        .contains(MapEntry.entry("baz", "qux"));
  }

  @Test public void group() {
    integration.group(new GroupPayloadBuilder().build());
    verifyNoMoreTapstreamInteractions();
  }

  @Test public void reset() {
    integration.reset();
    verifyNoMoreTapstreamInteractions();
  }

  private void verifyNoMoreTapstreamInteractions() {
    PowerMockito.verifyNoMoreInteractions(Tapstream.class);
    verifyNoMoreInteractions(tapstream);
  }

  static class EventMatcher extends TypeSafeMatcher<Event> {
    final String name;

    static Event eventEq(String name) {
      return argThat(new EventMatcher(name));
    }

    private EventMatcher(String name) {
      this.name = name;
    }

    public boolean matchesSafely(Event event) {
      return event.getName().compareTo(name) == 0;
    }

    protected void describeMismatchSafely(Event item, Description mismatchDescription) {
      super.describeMismatchSafely(item, mismatchDescription);
      mismatchDescription.appendText(item.getName());
      mismatchDescription.appendText(item.getEncodedName());
    }

    public void describeTo(Description description) {
      description.appendText(name);
    }
  }
}
