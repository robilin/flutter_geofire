import 'dart:async';
import 'package:flutter/services.dart';

class Geofire {
  static const MethodChannel _channel = MethodChannel('geofire');
  static const EventChannel _stream = EventChannel('geofireStream');

  static const onKeyEntered = "onKeyEntered";
  static const onGeoQueryReady = "onGeoQueryReady";
  static const onKeyMoved = "onKeyMoved";
  static const onKeyExited = "onKeyExited";

  static Stream<dynamic>? _queryAtLocation;

  static Future<bool> initialize(String path) async {
    final dynamic result = await _channel
        .invokeMethod('GeoFire.start', <String, dynamic>{"path": path});
    return result as bool? ?? false;
  }

  static Future<bool> setLocation(
      String id, double latitude, double longitude) async {
    final bool? isSet = await _channel.invokeMethod('setLocation',
        <String, dynamic>{"id": id, "lat": latitude, "lng": longitude});
    return isSet ?? false;
  }

  static Future<bool> removeLocation(String id) async {
    final bool? isSet = await _channel
        .invokeMethod('removeLocation', <String, dynamic>{"id": id});
    return isSet ?? false;
  }

  static Future<bool> stopListener() async {
    final bool? isSet =
        await _channel.invokeMethod('stopListener', <String, dynamic>{});
    return isSet ?? false;
  }

  static Future<Map<String, dynamic>> getLocation(String id) async {
    final Map<dynamic, dynamic>? response = await (_channel
        .invokeMethod('getLocation', <String, dynamic>{"id": id})) as Map<dynamic, dynamic>?;
    return response?.map<String, dynamic>((key, value) => MapEntry(key as String, value)) ?? {};
  }

  static Stream<dynamic>? queryAtLocation(
      double lat, double lng, double radius) {
    _channel.invokeMethod('queryAtLocation',
        {"lat": lat, "lng": lng, "radius": radius}).catchError((error) {
      // Handle error
    });

    _queryAtLocation ??= _stream.receiveBroadcastStream();
    return _queryAtLocation;
  }
}
