package com.example.taller2icm

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LocationActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mapa: GoogleMap
    private lateinit var clienteUbicacion: FusedLocationProviderClient
    private lateinit var callbackUbicacion: LocationCallback
    private var ultimaUbicacion: Location? = null
    private var marcador: Marker? = null

    private lateinit var manejadorSensores: SensorManager
    private var sensorLuz: Sensor? = null
    private var valorLuz: Float = 100f
    private var rutaActual: Polyline? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        val fragmentoMapa = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        fragmentoMapa.getMapAsync(this)

        clienteUbicacion = LocationServices.getFusedLocationProviderClient(this)
        manejadorSensores = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorLuz = manejadorSensores.getDefaultSensor(Sensor.TYPE_LIGHT)

        findViewById<EditText>(R.id.texto).setOnEditorActionListener { vista, _, _ ->
            val direccion = vista.text.toString()
            buscarDireccion(direccion)
            true
        }

        configurarCallbackUbicacion()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        habilitarUbicacion()

        mapa.setOnMapLongClickListener { posicion ->
            marcarYObtenerDireccion(posicion)
        }
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            habilitarUbicacion()
        } else {
            Toast.makeText(this, "Se necesita el permiso de ubicación", Toast.LENGTH_SHORT).show()
        }
    }



    private fun habilitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        mapa.isMyLocationEnabled = true

        clienteUbicacion.lastLocation.addOnSuccessListener { ubicacion ->
            if (ubicacion != null) {
                ultimaUbicacion = ubicacion
                actualizarMarcador(ubicacion)
                guardarEnJSON(ubicacion)
            }
        }

        clienteUbicacion.requestLocationUpdates(
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build(),
            callbackUbicacion,
            Looper.getMainLooper()
        )
    }



    private fun configurarCallbackUbicacion() {
        callbackUbicacion = object : LocationCallback() {
            override fun onLocationResult(resultado: LocationResult) {
                val ubicacionActual = resultado.lastLocation ?: return

                ultimaUbicacion?.let {
                    if (ubicacionActual.distanceTo(it) > 30) {
                        ultimaUbicacion = ubicacionActual
                        actualizarMarcador(ubicacionActual)
                        guardarEnJSON(ubicacionActual)
                    }
                } ?: run {
                    ultimaUbicacion = ubicacionActual
                    actualizarMarcador(ubicacionActual)
                    guardarEnJSON(ubicacionActual)
                }
            }
        }
    }

    private fun actualizarMarcador(ubicacion: Location) {
        val coordenadas = LatLng(ubicacion.latitude, ubicacion.longitude)
        if (marcador == null) {
            marcador = mapa.addMarker(MarkerOptions().position(coordenadas).title("Mi ubicación"))
        } else {
            marcador?.position = coordenadas
        }
        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(coordenadas, 17f))
    }

    private fun guardarEnJSON(ubicacion: Location) {
        val archivo = File(filesDir, "coordenadas.json")
        val arreglo = if (archivo.exists()) JSONArray(archivo.readText()) else JSONArray()
        val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val objeto = JSONObject().apply {
            put("lat", ubicacion.latitude)
            put("lon", ubicacion.longitude)
            put("hora", formatoFecha.format(Date()))
        }
        arreglo.put(objeto)
        archivo.writeText(arreglo.toString())
    }

    private fun buscarDireccion(direccion: String) {
        val geocoder = Geocoder(this)
        val resultados = geocoder.getFromLocationName(direccion, 1)

        if (resultados?.isNotEmpty() == true) {
            val ubicacion = resultados[0]
            val coordenadas = LatLng(ubicacion.latitude, ubicacion.longitude)

            val direccionCompleta = ubicacion.getAddressLine(0) ?: direccion

            mapa.addMarker(MarkerOptions().position(coordenadas).title(direccionCompleta))
            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(coordenadas, 16f))
            mostrarDistancia(coordenadas)
            ultimaUbicacion?.let {
                val origen = LatLng(it.latitude, it.longitude)
                dibujarRuta(origen, coordenadas)
            }

        } else {
            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
        }
    }


    private fun marcarYObtenerDireccion(coordenadas: LatLng) {
        val geocoder = Geocoder(this)
        val resultados = geocoder.getFromLocation(coordenadas.latitude, coordenadas.longitude, 1)
        val titulo = resultados?.firstOrNull()?.getAddressLine(0) ?: "Ubicación sin nombre"
        mapa.addMarker(MarkerOptions().position(coordenadas).title(titulo))
        mostrarDistancia(coordenadas)
        ultimaUbicacion?.let {
            val origen = LatLng(it.latitude, it.longitude)
            dibujarRuta(origen, coordenadas)
        }

    }

    private fun mostrarDistancia(destino: LatLng) {
        ultimaUbicacion?.let {
            val origen = LatLng(it.latitude, it.longitude)
            val resultado = FloatArray(1)
            Location.distanceBetween(
                origen.latitude, origen.longitude,
                destino.latitude, destino.longitude,
                resultado
            )
            Toast.makeText(
                this,
                "Distancia: %.2f metros".format(resultado[0]),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onSensorChanged(evento: SensorEvent?) {
        if (evento?.sensor?.type == Sensor.TYPE_LIGHT) {
            valorLuz = evento.values[0]
            if (::mapa.isInitialized) {
                val estilo = if (valorLuz < 100) R.raw.dark else R.raw.retro
                mapa.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, estilo))
            }
        }
    }


    private fun dibujarRuta(origen: LatLng, destino: LatLng) {
        val roadManager = org.osmdroid.bonuspack.routing.OSRMRoadManager(this, "Android")
        val waypoints = arrayListOf(
            org.osmdroid.util.GeoPoint(origen.latitude, origen.longitude),
            org.osmdroid.util.GeoPoint(destino.latitude, destino.longitude)
        )

        Thread {
            try {
                val road = roadManager.getRoad(waypoints)
                val puntos = road.mRouteHigh.map { LatLng(it.latitude, it.longitude) }

                runOnUiThread {
                    rutaActual?.remove()

                    rutaActual = mapa.addPolyline(
                        PolylineOptions()
                            .addAll(puntos)
                            .width(10f)
                            .color(android.graphics.Color.BLUE)
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error al generar ruta", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }



    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorLuz?.let {
            manejadorSensores.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        manejadorSensores.unregisterListener(this)
    }
}
