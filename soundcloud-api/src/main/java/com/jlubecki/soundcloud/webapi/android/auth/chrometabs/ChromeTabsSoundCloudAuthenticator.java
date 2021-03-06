/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jacob Lubecki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.jlubecki.soundcloud.webapi.android.auth.chrometabs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsSession;
import com.jlubecki.soundcloud.webapi.android.auth.AuthenticationCallback;
import com.jlubecki.soundcloud.webapi.android.auth.SoundCloudAuthenticator;

/**
 * Class which contains relevant methods for authenticating a user to make full use of the
 * SoundCloud API. A user will have to add an intent filter for their app's callback URI in
 * AndroidManifest.xml for the activity that should handle the Auth Token. Tokens are set to be
 * non-expiring by default but this does not guarantee their longevity. Please see the README file
 * for the soundcloud-web-api-android module for more info.
 */
public class ChromeTabsSoundCloudAuthenticator extends SoundCloudAuthenticator {

  private final Activity context;
  private final String browserPackageName;
  private final AuthTabServiceConnection serviceConnection;

  private CustomTabsIntent.Builder tabsIntentBuilder;

  /**
   * Creates a {@link SoundCloudAuthenticator} that launches a Chrome Custom Tab to handle authentication.
   *
   * @param clientId Client ID of the application requesting authorization.
   * @param redirectUri Redirect URI of the application requesting authorization
   * @param context Activity from which authentication is being launched.
   * @param serviceConnection A Service Connection that establishes a relationship between the
   *                          Chrome Custom Tab and the authenticating application.
   */
  public ChromeTabsSoundCloudAuthenticator(String clientId, String redirectUri, Activity context, AuthTabServiceConnection serviceConnection) {
    super(clientId, redirectUri);

    this.context = context;
    this.serviceConnection = serviceConnection;
    this.browserPackageName = CustomTabsClient.getPackageName(context, null);
  }

  /**
   * Same as {@link #ChromeTabsSoundCloudAuthenticator(String, String, Activity, AuthTabServiceConnection)}
   * but launches the ChromeTab as soon as the Service Connection is established.
   *
   * @param clientId Client ID of the application requesting authorization.
   * @param redirectUri Redirect URI of the application requesting authorization
   * @param context Activity from which authentication is being launched.
   */
  public ChromeTabsSoundCloudAuthenticator(String clientId, String redirectUri, Activity context) {
    super(clientId, redirectUri);

    this.context = context;
    this.serviceConnection = new AuthTabServiceConnection(new AuthenticationCallback() {
      @Override public void onReadyToAuthenticate() {
        launchAuthenticationFlow();
      }

      @Override public void onAuthenticationEnded() {
        // Do nothing.
      }
    });

    this.browserPackageName = CustomTabsClient.getPackageName(context, null);
  }

  /**
   * Attempts to bind a CustomTabsServiceConnection that will notify the user when the authentication
   * is ready and when the connection is ended.
   *
   * @return whether or not the CustomTabsClient could bind the Service.
   */
  @Override public boolean prepareAuthenticationFlow() {
    serviceConnection.setClientAuthUrl(loginUrl());

    return browserPackageName != null &&
        CustomTabsClient.bindCustomTabsService(context, browserPackageName, serviceConnection);
  }

  /**
   * Uses the provided CustomTabsIntent.Builder or the default implementation to create and launch
   * a CustomTabsIntent that connects to the SoundCloud authentication website.
   */
  @Override public void launchAuthenticationFlow() {
    if(tabsIntentBuilder == null) {
      tabsIntentBuilder = newTabsIntentBuilder();
    }

    CustomTabsIntent tabsIntent = tabsIntentBuilder.build();
    addReferrerToIntent(tabsIntent.intent, context.getPackageName());
    tabsIntent.intent.setPackage(browserPackageName);
    tabsIntent.launchUrl(context, Uri.parse(loginUrl()));
  }

  /**
   * Allows a user to customize their chrome tab to reflect their application's specific needs.
   *
   * @param newBuilder Defines the builder to use for creating the customTabsIntent that will launch
   *                   the authentication flow.
   */
  public void setTabsIntentBuilder(@NonNull CustomTabsIntent.Builder newBuilder) {
    this.tabsIntentBuilder = newBuilder;
  }

  /**
   * Gets a Builder that will allow a user to define the look and feel of the Custom Tab that handles
   * the authentication flow. Should only be called after an {@link AuthenticationCallback} notifies
   * that the authentication is ready to begin with {@link AuthenticationCallback#onReadyToAuthenticate()}.
   *
   * @return a builder that can be passed back to {@link #setTabsIntentBuilder(CustomTabsIntent.Builder)}
   *         when it has been customized as desired.
   */
  public CustomTabsIntent.Builder newTabsIntentBuilder() {
    CustomTabsSession tabsSession = serviceConnection.getSession();

    return new CustomTabsIntent.Builder(tabsSession);
  }

  /**
   * Should be called when authentication is finished or if the authentication process is no longer
   * needed.
   */
  public void unbindService() {
    context.unbindService(serviceConnection);
  }
}
