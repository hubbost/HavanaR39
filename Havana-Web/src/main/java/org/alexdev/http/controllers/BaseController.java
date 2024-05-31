package org.alexdev.http.controllers;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.alexdev.duckhttpd.routes.Route;
import org.alexdev.duckhttpd.server.connection.WebConnection;
import org.alexdev.havana.util.DateUtil;
import org.alexdev.havana.util.config.GameConfiguration;
import org.alexdev.http.Routes;
import org.alexdev.http.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseController implements Route {
    private static Logger logger = LoggerFactory.getLogger(BaseController.class);

    @Override
    public void handleRoute(WebConnection webConnection) throws Exception {
        if (webConnection.isRequestHandled()) {
            if (GameConfiguration.getInstance().getBoolean("maintenance") && !webConnection.getRouteRequest().startsWith("/api")) {
                if (!webConnection.getRouteRequest().startsWith("/maintenance") && !webConnection.getRouteRequest().startsWith("/" + Routes.HOUSEKEEPING_PATH)) {
                    webConnection.redirect("/maintenance");
                    return;
                }
            }
        }

        if (!webConnection.getRouteRequest().startsWith("/api")) {
            if (!webConnection.request().headers().isEmpty()) {
                String host = webConnection.request().headers().get(HttpHeaderNames.HOST);

                if (webConnection.request().headers().contains("X-Forwarded-Proto")) {
                    String request = webConnection.request().headers().get("X-Forwarded-Proto");

                    if (host != null && request.equalsIgnoreCase("http")) {
                        String targetUrl = "https://" + host;
                        String requestUri = webConnection.request().uri();

                        if (!requestUri.startsWith("/")) {
                            targetUrl += "/";
                        }

                        targetUrl += requestUri;

                        webConnection.movedpermanently(targetUrl);
                        return;
                    }
                }
            }
        }

        if (webConnection.isRequestHandled()) {
            if (webConnection.session().getBoolean("authenticated")) {
                this.handleAuthenticatedRoute(webConnection);
            } else {
                SessionUtil.checkCookie(webConnection);
            }
        }
    }

    private void handleAuthenticatedRoute(WebConnection webConnection) {
        if (webConnection.getRouteRequest().equals("/client")) {
            webConnection.session().set("lastRequest", String.valueOf(DateUtil.getCurrentTimeSeconds() + SessionUtil.REAUTHENTICATE_TIME));
        }

        if (webConnection.session().contains("lastRequest")) {
            long lastRequest = webConnection.session().getLongOrElse("lastRequest", 0);

            if (DateUtil.getCurrentTimeSeconds() > lastRequest) {
                webConnection.session().set("clientAuthenticate", true);
            }
        } else {
            webConnection.session().set("clientAuthenticate", false);
        }
    }
}
