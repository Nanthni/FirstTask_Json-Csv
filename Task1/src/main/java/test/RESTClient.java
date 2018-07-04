package test;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class RESTClient {
    private static final Class <?> thisClass = new Object () {
    }.getClass ().getEnclosingClass ();
//	private static Logger logger = LogManager.getLogger(thisClass);

    private WebTarget target;
    private URL baseUrl;
    private String defaultMediaType;
    private Map <String, String> headers = new HashMap <> ();

    public static RESTClient newClient(URL baseUrl) {
        return new RESTClient ( baseUrl, MediaType.APPLICATION_JSON, false );
    }

    public static RESTClient newClient(URL baseUrl, String defaultMediaType) {
        return new RESTClient ( baseUrl, defaultMediaType, false );
    }

    public static RESTClient newCookieClient(URL baseUrl) {
        return new RESTClient ( baseUrl, MediaType.APPLICATION_JSON, true );
    }

    public static RESTClient newCookieClient(URL baseUrl, String defaultMediaType) {
        return new RESTClient ( baseUrl, defaultMediaType, true );
    }

    //

    private RESTClient(URL baseUrl, String defaultMediaType, boolean useCookies) {
        this.target = createClientTarget ( baseUrl, useCookies );
        this.baseUrl = baseUrl;
        this.defaultMediaType = defaultMediaType;
    }

    public WebTarget getTarget() {
        return target;
    }

    public void setTarget(WebTarget target) {
        this.target = target;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultMediaType() {
        return defaultMediaType;
    }

    public void setDefaultMediaType(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    public Map <String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map <String, String> headers) {
        this.headers = headers;
    }

    public void setHeaders(List <String> headers) {
        this.headers = new HashMap <> ();
        if (headers.size () == 0) return;

        // Example header strings: Authorization: Basic A1B2C3; Accept: application/json; NoValueHeader; etc.
        for (String header : headers) {
            String[] a = header.split ( "\\s*:\\s*", 2 );
            String key = (a.length > 0) ? a[0] : null;
            String value = (a.length > 1) ? a[1] : null;
            if (key != null) this.headers.put ( key, value );
        }
    }

    public void setHeaders(String headerString) {
        this.headers = new HashMap <> ();
        if (StringUtils.isBlank ( headerString )) return;

        // Example header string: Authorization: Basic A1B2C3; Accept: application/json; NoValueHeader; etc.
        for (String header : headerString.split ( "\\s*;\\s*" )) {
            String[] a = header.split ( "\\s*:\\s*" );
            String key = (a.length > 0) ? a[0] : null;
            String value = (a.length > 1) ? a[1] : null;
            if (key != null) this.headers.put ( key, value );
        }
    }

    // Jersey Helpers

    public Builder createBuilder(String path) {
        return createBuilder ( path, defaultMediaType );
    }

    public Builder createBuilder(String path, String acceptType) {
//		logger.trace(path);

        WebTarget t = target;

        if (path.contains ( "?" )) {
            int pos = path.indexOf ( "?" );
            t = target.path ( path.substring ( 0, pos ) );
            String[] params = path.substring ( pos + 1 ).split ( "&" );
            for (String param : params) {
                String[] a = param.split ( "=", 2 );
                t = t.queryParam ( a[0], a[1] );
            }
        } else {
            t = target.path ( path );
        }

        Builder b = t.request ( acceptType );

        if (headers != null || headers.size () > 0) {
            for (String key : headers.keySet ()) {
                b = b.header ( key, headers.get ( key ) );
            }
        }

        return b;
    }

    private WebTarget createClientTarget(URL baseUrl, boolean useCookies) {
        ClientConfig clientConfig = new ClientConfig ()
                .register ( MultiPartFeature.class )
                .register ( JacksonJsonProvider.class )
//				.register(JacksonXMLProvider.class)
//				.register(JacksonYAMLProvider.class)
//				.register(JacksonCBORProvider.class)
                ;

        if (!baseUrl.getProtocol ().toLowerCase ().equalsIgnoreCase ( "https" )) {
            return ClientBuilder.newClient ( clientConfig ).target ( baseUrl.toString () );
        }

        TrustManager[] certs = new TrustManager[]{new X509TrustManager () {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
        }};

        try {
            SSLContext ctx = SSLContext.getInstance ( "TLS" );
            ctx.init ( null, certs, new SecureRandom () );

            ClientBuilder clientBuilder = ClientBuilder.newBuilder ();
            clientBuilder.sslContext ( ctx );
            clientBuilder.hostnameVerifier ( new HostnameVerifier () {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            } );

            Client client = clientBuilder.withConfig ( clientConfig ).build ();
            if (useCookies) client = client.register ( new CookieClientFilter () );
            return client.target ( baseUrl.toString () );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException ( e );
        } catch (KeyManagementException e) {
            throw new RuntimeException ( e );
        }
    }

    private class CookieClientFilter implements ClientRequestFilter, ClientResponseFilter {
        private final ArrayList <Object> cookies = new ArrayList <Object> ();

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            for (NewCookie newCookie : responseContext.getCookies ().values ()) {
                cookies.add ( newCookie.toCookie () );
            }
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            if (cookies == null || cookies.size () == 0) return;
            requestContext.getHeaders ().put ( "Cookie", cookies );
        }
    }
}


