package com.segment.analytics.android.integrations.tapstream;

import android.util.Log;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.tapstream.sdk.Config;
import com.tapstream.sdk.Event;
import com.tapstream.sdk.Logging;
import com.tapstream.sdk.Tapstream;
import java.util.Map;

import static com.segment.analytics.Analytics.LogLevel.INFO;
import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Tapstream is a mobile attribution tool that lets you attribute app installs to individual users
 * who have visited your website, so your marketing team can know what's working.
 *
 * @see <a href="https://tapstream.com">Tapstream</a>
 * @see <a href="https://segment.com/docs/integrations/tapstream/">Tapstream Integration</a>
 * @see <a href="https://tapstream.com/developer/android-sdk-documentation/">Tapstream Android
 *     SDK</a>
 */
public class TapstreamIntegration extends Integration<Tapstream> {
  public static final Factory FACTORY =
      new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, Analytics analytics) {
          return new TapstreamIntegration(analytics, settings);
        }

        @Override
        public String key() {
          return TAPSTREAM_KEY;
        }
      };

  private static final String TAPSTREAM_KEY = "Tapstream";
  private static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";

  boolean trackAllPages;
  boolean trackCategorizedPages;
  boolean trackNamedPages;
  final Tapstream tapstream;
  Config config;
  final Logger logger;

  TapstreamIntegration(Analytics analytics, ValueMap settings) {
    trackAllPages = settings.getBoolean("trackAllPages", true);
    trackCategorizedPages = settings.getBoolean("trackCategorizedPages", true);
    trackNamedPages = settings.getBoolean("trackNamedPages", true);

    logger = analytics.logger(TAPSTREAM_KEY);
    if (logger.logLevel == INFO || logger.logLevel == VERBOSE) {
      Logging.setLogger(
          new com.tapstream.sdk.Logger() {
            @Override
            public void log(int logLevel, String msg) {
              Log.d(TAPSTREAM_KEY, msg);
            }
          });
    }

    String accountName = settings.getString("accountName");
    String sdkSecret = settings.getString("sdkSecret");
    config = new Config();
    Tapstream.create(analytics.getApplication(), accountName, sdkSecret, config);
    tapstream = Tapstream.getInstance();
  }

  @Override
  public Tapstream getUnderlyingInstance() {
    return tapstream;
  }

  @Override
  public void track(TrackPayload track) {
    super.track(track);

    fireEvent(track.event(), track.properties());
  }

  @Override
  public void screen(ScreenPayload screen) {
    super.screen(screen);

    if (trackAllPages) {
      fireEvent(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
      return;
    }

    if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      fireEvent(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
      return;
    }

    if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      fireEvent(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  private void fireEvent(String name, Properties properties) {
    Event event = new Event(name, false);
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      event.addPair(entry.getKey(), entry.getValue());
    }
    tapstream.fireEvent(event);
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);
    for (Map.Entry<String, Object> entry : identify.traits().entrySet()) {
      config.globalEventParams.put(entry.getKey(), entry.getValue());
    }
  }
}
