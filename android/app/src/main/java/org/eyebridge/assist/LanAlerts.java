package org.eyebridge.assist;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Trusted-LAN-only, unauthenticated except for short shared pairing code.
 * DO NOT expose this UDP port outside a private Wi-Fi network.
 */
public final class LanAlerts {
    public static final int PORT = 9876;
    public interface Listener { void onAlert(String type, String id, String source); void onError(String info); }
    public interface Delivery { void done(boolean receivedByApp, String explanation); }
    private final ExecutorService io = Executors.newCachedThreadPool();
    private volatile DatagramSocket receiver;
    private final HashSet<String> seen = new HashSet<>();
    private final ArrayDeque<String> recent = new ArrayDeque<>();

    public static String localIps() {
        StringBuilder sb = new StringBuilder();
        try {
            for (NetworkInterface net : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!net.isUp() || net.isLoopback()) continue;
                for (InetAddress ip : Collections.list(net.getInetAddresses())) {
                    if (ip instanceof Inet4Address && ip.isSiteLocalAddress()) {
                        if (sb.length() != 0) sb.append("   ");
                        sb.append(ip.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) { }
        return sb.length() > 0 ? sb.toString() : "Check Wi-Fi IPv4 address in phone settings";
    }
    public synchronized void stopReceiver() {
        DatagramSocket old = receiver;
        receiver = null;
        if (old != null) old.close();
    }
    public synchronized void startReceiver(String code, Listener listener) throws Exception {
        if (!code.matches("[0-9]{6}")) throw new IllegalArgumentException("Enter a 6-digit pairing code");
        stopReceiver();
        seen.clear(); recent.clear();
        DatagramSocket sock = new DatagramSocket(PORT);
        receiver = sock;
        io.execute(() -> {
            byte[] bytes = new byte[512];
            while (receiver == sock && !sock.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    sock.receive(packet);
                    String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    String[] bits = msg.split("\\|", -1);
                    if (bits.length != 4 || !bits[0].equals("EB1") || !bits[1].equals(code) ||
                        !bits[2].matches("[a-zA-Z0-9-]{8,48}") ||
                        !(bits[3].equals("PING") || bits[3].equals("COLD") || bits[3].equals("HELP") ||
                          bits[3].equals("EMERGENCY") || bits[3].equals("TEST"))) continue;
                    String ack = "ACK|" + code + "|" + bits[2];
                    byte[] response = ack.getBytes(StandardCharsets.UTF_8);
                    sock.send(new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort()));
                    if (bits[3].equals("PING")) {
                        listener.onAlert("PAIRING", bits[2], packet.getAddress().getHostAddress());
                        continue;
                    }
                    synchronized (seen) {
                        if (seen.contains(bits[2])) continue;
                        seen.add(bits[2]); recent.addLast(bits[2]);
                        if (recent.size() > 100) seen.remove(recent.removeFirst());
                    }
                    listener.onAlert(bits[3], bits[2], packet.getAddress().getHostAddress());
                } catch (Exception ex) {
                    if (!sock.isClosed()) listener.onError("Receiver stopped: " + ex.getMessage());
                }
            }
        });
    }
    public void send(String ip, String code, String type, Delivery listener) {
        if (!code.matches("[0-9]{6}") || !ip.matches("[0-9.]{7,15}") ||
            !(type.equals("PING") || type.equals("COLD") || type.equals("HELP") ||
              type.equals("EMERGENCY") || type.equals("TEST"))) {
            listener.done(false,"Please enter the caregiver's Wi-Fi IPv4 and 6-digit code"); return;
        }
        io.execute(() -> {
            try (DatagramSocket sock = new DatagramSocket()) {
                sock.setSoTimeout(400);
                InetAddress dest = InetAddress.getByName(ip);
                if (!(dest instanceof Inet4Address) || !dest.isSiteLocalAddress()) throw new IllegalArgumentException("Use a private local Wi-Fi IPv4 address");
                String id = UUID.randomUUID().toString();
                byte[] payload = ("EB1|"+code+"|"+id+"|"+type).getBytes(StandardCharsets.UTF_8);
                for (int i=0;i<9;i++) {
                    sock.send(new DatagramPacket(payload,payload.length,dest,PORT));
                    try {
                        DatagramPacket response = new DatagramPacket(new byte[128],128);
                        sock.receive(response);
                        String ack = new String(response.getData(),0,response.getLength(),StandardCharsets.UTF_8);
                        if (response.getAddress().equals(dest) && ack.equals("ACK|"+code+"|"+id)) {
                            listener.done(true,"Received by caregiver APP (not a human acknowledgment)");
                            return;
                        }
                    } catch (SocketTimeoutException ignored) { }
                }
                listener.done(false,"NOT CONFIRMED: no acknowledgment from caregiver app. Use another way to communicate.");
            } catch (Exception ex) { listener.done(false,"Network error: " + ex.getMessage()); }
        });
    }
    public void close() { stopReceiver(); io.shutdownNow(); }
}
