package in.appyflow.geofire;

import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.LocationCallback;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * GeofirePlugin
 */
public class GeofirePlugin implements FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

    private GeoFire geoFire;
    private DatabaseReference databaseReference;
    private static MethodChannel channel;
    private static EventChannel eventChannel;
    private EventChannel.EventSink events;
    private GeoQuery geoQuery;
    private final HashMap<String, Object> hashMap = new HashMap<>();

    /**
     * Initialize the plugin (called in onAttachedToEngine).
     */
    public static void pluginInit(BinaryMessenger messenger) {
        GeofirePlugin geofirePlugin = new GeofirePlugin();

        channel = new MethodChannel(messenger, "geofire");
        channel.setMethodCallHandler(geofirePlugin);

        eventChannel = new EventChannel(messenger, "geofireStream");
        eventChannel.setStreamHandler(geofirePlugin);
    }

    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        pluginInit(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        // Clean up channels
        if (channel != null) {
            channel.setMethodCallHandler(null);
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MethodCallHandler
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        Log.i("GeoFirePlugin", "Method call: " + call.method);

        switch (call.method) {
            case "GeoFire.start":
                startGeoFire(call, result);
                break;
            case "setLocation":
                setLocation(call, result);
                break;
            case "removeLocation":
                removeLocation(call, result);
                break;
            case "getLocation":
                getLocation(call, result);
                break;
            case "queryAtLocation":
                queryAtLocation(call, result);
                break;
            case "stopListener":
                stopListener(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void startGeoFire(MethodCall call, Result result) {
        String path = call.argument("path");
        databaseReference = FirebaseDatabase.getInstance().getReference(path);
        geoFire = new GeoFire(databaseReference);

        if (geoFire.getDatabaseReference() != null) {
            result.success(true);
        } else {
            result.success(false);
        }
    }

    private void setLocation(MethodCall call, final Result result) {
        String id = call.argument("id");
        double lat = Double.parseDouble(call.argument("lat").toString());
        double lng = Double.parseDouble(call.argument("lng").toString());

        geoFire.setLocation(id, new GeoLocation(lat, lng), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    result.success(false);
                } else {
                    result.success(true);
                }
            }
        });
    }

    private void removeLocation(MethodCall call, final Result result) {
        String id = call.argument("id");

        geoFire.removeLocation(id, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    result.success(false);
                } else {
                    result.success(true);
                }
            }
        });
    }

    private void getLocation(MethodCall call, final Result result) {
        String id = call.argument("id");

        geoFire.getLocation(id, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                HashMap<String, Object> map = new HashMap<>();
                if (location != null) {
                    map.put("lat", location.latitude);
                    map.put("lng", location.longitude);
                    map.put("error", null);
                } else {
                    map.put("error", String.format("There is no location for key %s in GeoFire", key));
                }
                result.success(map);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("error", "Error getting the GeoFire location: " + databaseError);
                result.success(map);
            }
        });
    }

    private void queryAtLocation(MethodCall call, final Result result) {
        double lat = Double.parseDouble(call.argument("lat").toString());
        double lng = Double.parseDouble(call.argument("lng").toString());
        double radius = Double.parseDouble(call.argument("radius").toString());
        geoFireArea(lat, lng, result, radius);
    }

    private void geoFireArea(final double latitude, final double longitude,
                             final Result result, final double radius) {
        try {
            final ArrayList<String> arrayListKeys = new ArrayList<>();

            if (geoQuery != null) {
                geoQuery.setLocation(new GeoLocation(latitude, longitude), radius);
            } else {
                geoQuery = geoFire.queryAtLocation(new GeoLocation(latitude, longitude), radius);
            }

            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    if (events != null) {
                        hashMap.clear();
                        hashMap.put("callBack", "onKeyEntered");
                        hashMap.put("key", key);
                        hashMap.put("latitude", location.latitude);
                        hashMap.put("longitude", location.longitude);
                        events.success(hashMap);
                    } else {
                        geoQuery.removeAllListeners();
                    }
                    arrayListKeys.add(key);
                }

                @Override
                public void onKeyExited(String key) {
                    arrayListKeys.remove(key);
                    if (events != null) {
                        hashMap.clear();
                        hashMap.put("callBack", "onKeyExited");
                        hashMap.put("key", key);
                        events.success(hashMap);
                    } else {
                        geoQuery.removeAllListeners();
                    }
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    if (events != null) {
                        hashMap.clear();
                        hashMap.put("callBack", "onKeyMoved");
                        hashMap.put("key", key);
                        hashMap.put("latitude", location.latitude);
                        hashMap.put("longitude", location.longitude);
                        events.success(hashMap);
                    } else {
                        geoQuery.removeAllListeners();
                    }
                }

                @Override
                public void onGeoQueryReady() {
                    if (events != null) {
                        hashMap.clear();
                        hashMap.put("callBack", "onGeoQueryReady");
                        hashMap.put("result", arrayListKeys);
                        events.success(hashMap);
                    } else {
                        geoQuery.removeAllListeners();
                    }
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    if (events != null) {
                        events.error("Error", "GeoQueryError", error);
                    } else {
                        geoQuery.removeAllListeners();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            result.error("Error", "General Error", e);
        }
    }

    private void stopListener(Result result) {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
        result.success(true);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // EventChannel.StreamHandler
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    public void onListen(Object arguments, EventChannel.EventSink eventSink) {
        this.events = eventSink;
    }

    @Override
    public void onCancel(Object arguments) {
        if (geoQuery != null) {
            geoQuery.removeAllListeners();
        }
        this.events = null;
    }
}
