package com.sharedfund.app.data;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

public class SafeDns implements Dns, org.java_websocket.client.DnsResolver {

    private static final SafeDns INSTANCE = new SafeDns();

    public static SafeDns getInstance() {
        return INSTANCE;
    }

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        try {
            return Dns.SYSTEM.lookup(hostname);
        } catch (UnknownHostException original) {
            if ("nagirshltkzuxqztapms.supabase.co".equalsIgnoreCase(hostname)) {
                try {
                    return Arrays.asList(
                            InetAddress.getByAddress(hostname, new byte[]{(byte) 172, 64, (byte) 149, (byte) 246}),
                            InetAddress.getByAddress(hostname, new byte[]{104, 18, 38, 10})
                    );
                } catch (Exception ignored) {
                }
            } else if ("qr.sepay.vn".equalsIgnoreCase(hostname)) {
                try {
                    return Arrays.asList(
                            InetAddress.getByAddress(hostname, new byte[]{104, 20, 41, 46}),
                            InetAddress.getByAddress(hostname, new byte[]{(byte) 172, 66, (byte) 160, (byte) 132})
                    );
                } catch (Exception ignored) {
                }
            } else if ("api.vietqr.io".equalsIgnoreCase(hostname) || "img.vietqr.io".equalsIgnoreCase(hostname)) {
                try {
                    return Arrays.asList(
                            InetAddress.getByAddress(hostname, new byte[]{104, 26, 15, (byte) 229}),
                            InetAddress.getByAddress(hostname, new byte[]{104, 26, 14, (byte) 229})
                    );
                } catch (Exception ignored) {
                }
            }
            throw original;
        }
    }

    @Override
    public InetAddress resolve(URI uri) throws UnknownHostException {
        List<InetAddress> list = lookup(uri.getHost());
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        throw new UnknownHostException("Unable to resolve host: " + uri.getHost());
    }
}
