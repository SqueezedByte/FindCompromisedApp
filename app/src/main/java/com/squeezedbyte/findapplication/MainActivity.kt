package com.squeezedbyte.findapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.squeezedbyte.findapplication.classiVarie.ClassiDiComodo.ListaApp
import com.squeezedbyte.findapplication.ui.theme.FindApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException



class MainActivity : ComponentActivity() {
    private lateinit var dialog: AlertDialog
    private var listaPacchettiLaidi = mutableListOf<ListaApp>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listaPacchetti = ottieniListaPacchetti()
        dialog = mostraPB(this@MainActivity, "Carico dati remoti")
        dialog.show()
        CoroutineScope(Dispatchers.IO).launch {

            val listaAppLaide= ottieniListaGDrive()
            if(listaAppLaide.isNullOrEmpty()){
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "Errore nel recupero dei dati", Toast.LENGTH_LONG).show()
                }
                return@launch
            }
            listaPacchetti?.forEach { appInstallata ->
                if (listaAppLaide.any { it.nomeApp == appInstallata.nomeApp || it.nomePacchetto == appInstallata.nomePacchetto}) {
                    listaPacchettiLaidi.add(appInstallata)
                }
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                setContent {
                    FindApplicationTheme {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            PackageList(pacchetti = listaPacchettiLaidi)
                        }
                    }
                }
            }
        }
    }

    private fun ottieniListaPacchetti(): List<ListaApp> {
        val packageManager = this.packageManager
        return packageManager.getInstalledPackages(0).map { packageInfo ->
            ListaApp(
                nomeApp = packageManager.getApplicationLabel(packageInfo.applicationInfo!!).toString(),
                nomePacchetto = packageInfo.packageName
            )
        }
    }

    private fun ottieniListaGDrive(): List<ListaApp>? {
        var listaApp = mutableListOf<ListaApp>()
        val url = "https://spreadsheets.google.com/tq?key=1Ukgd0gIWd9gpV6bOx2pcSHsVO6yIUqbjnlM4ewjO6Cs"
        val httpAsync = url
            .httpGet()
            .responseString { _, response, result ->
                when (result) {
                    is com.github.kittinunf.result.Result.Failure -> {
                        val ex = result.getException()
                    }
                    is Result.Success -> {
                        var datorisolto = result.get()
                        datorisolto = datorisolto.substringAfter("\"pattern\":\"General\"}],").substringBeforeLast(",\"parsedNumHeaders\":1}});")
                        datorisolto = "{${datorisolto}}"
                        if(datorisolto.isNotEmpty()){
                            try {
                                val json = org.json.JSONObject(datorisolto)
                                val rows = json.getJSONArray("rows")
                                for (i in 0 until rows.length()) {
                                    val row = rows.getJSONObject(i)
                                    val c = row.getJSONArray("c")
                                    val nomeApp = c.getJSONObject(0).getString("v")
                                    val nomePacchetto = c.getJSONObject(1).getString("v")
                                    listaApp.add(ListaApp(nomeApp, nomePacchetto))
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
        httpAsync.join()
        return listaApp
    }
    @SuppressLint("UseCompatLoadingForDrawables", "MissingInflatedId")
    fun mostraPB(context: Context, message:String): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        val messageBoxView = LayoutInflater.from(context).inflate(R.layout.barradiprogresso,null)
        builder.setView(messageBoxView)
        val pizzaDropdown = messageBoxView.findViewById<TextView>(R.id.testoPB)
        pizzaDropdown.text = message
        builder.create()
        return builder.create()
    }
    fun clickapri(dato: String) {
        copyToClipboard(dato)
    }
    fun copyToClipboard(text: String) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$text")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
    @Composable
    fun PackageList(pacchetti:List<ListaApp>) {
        Column(
            modifier = Modifier.padding(top = 56.dp, start = 10.dp).fillMaxSize().background(Color.White)
        ) {
            pacchetti.forEach { nomePacchetto ->
                var testo =if(nomePacchetto.nomeApp.isNotEmpty()) {
                    nomePacchetto.nomeApp
                }else {
                    nomePacchetto.nomePacchetto
                }
                Text(text = testo,color = Color.Black, modifier = Modifier.padding(top = 10.dp, start = 5.dp).clickable { clickapri(nomePacchetto.nomePacchetto) })
            }
        }
    }
}

