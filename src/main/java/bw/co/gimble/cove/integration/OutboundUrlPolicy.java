package bw.co.gimble.cove.integration;

import bw.co.gimble.cove.common.ApiException;
import bw.co.gimble.cove.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;

/**
 * Guards every URL Cove calls on a company's behalf (media imports, webhooks)
 * against SSRF: public http(s) hosts only, never loopback, private, link-local
 * or metadata addresses. Local development can opt out with
 * app.integration.allow-private-hosts=true.
 */
@Component
public class OutboundUrlPolicy {

    @Value("${app.integration.allow-private-hosts:false}")
    private boolean allowPrivateHosts;

    public URI require(String raw, boolean httpsOnly) {
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (Exception e) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid URL: " + raw);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        boolean https = scheme.equals("https");
        if (!https && !(scheme.equals("http") && (!httpsOnly || allowPrivateHosts))) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED,
                    (httpsOnly ? "Only https URLs are allowed: " : "Only http(s) URLs are allowed: ") + raw);
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid URL: " + raw);
        }
        if (!allowPrivateHosts) {
            try {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                            || address.isSiteLocalAddress() || address.isMulticastAddress()
                            || isUniqueLocalV6(address) || isCarrierGradeNat(address)) {
                        throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "That host is not reachable from Cove: " + uri.getHost());
                    }
                }
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Could not resolve host: " + uri.getHost());
            }
        }
        return uri;
    }

    private static boolean isUniqueLocalV6(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 16 && (b[0] & 0xfe) == 0xfc;
    }

    private static boolean isCarrierGradeNat(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 4 && (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 64;
    }
}
