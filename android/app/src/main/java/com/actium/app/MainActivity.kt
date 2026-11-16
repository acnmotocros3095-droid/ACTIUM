package com.actium.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

typealias Records = Map<String,List<JSONObject>>
typealias SaveRecord = (String,JSONObject,()->Unit)->Unit
private val tables=listOf("meals","meal_items","exercises","workout_sessions","workout_sets","water_entries","sleep_entries","body_measurements","daily_goals")

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.rgb(13,17,21)),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.rgb(13,17,21)))
        setContent { ActiumTheme { ActiumApp() } }
    }
}

@Composable internal fun ActiumApp() {
    val api=remember{Api()};val scope=rememberCoroutineScope()
    var logged by remember{mutableStateOf(false)}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    var notice by remember{mutableStateOf("")}
    var page by rememberSaveable{mutableStateOf("Inicio")}
    var dateText by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
    var data by remember{mutableStateOf<Records>(emptyMap())}
    suspend fun reload() {
        val fetched=withContext(Dispatchers.IO){coroutineScope{tables.associateWith{table->async{api.rows(table)}}.mapValues{it.value.await()}}}
        data=fetched
    }
    fun refresh() {
        if(busy)return
        busy=true;error="";notice=""
        scope.launch{try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudieron actualizar los registros"}finally{busy=false}}
    }
    val save:SaveRecord={table,record,onSuccess->
        if(!busy){busy=true;error="";notice="";scope.launch{
            try{withContext(Dispatchers.IO){api.add(table,record)};onSuccess();notice="Registro guardado";try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){error="El registro se guardó, pero no se pudo actualizar la vista. Pulsa actualizar."}}
            catch(e:CancellationException){throw e}
            catch(e:Exception){error=e.message?:"No se pudo guardar. Revisa tu conexión."}
            finally{busy=false}
        }}
    }
    CompositionLocalProvider(LocalSaving provides busy) {
        if(!api.configured) {
            Box(Modifier.fillMaxSize().background(Ink).padding(24.dp),contentAlignment=Alignment.Center){Tile{
                Glyph("spark",Lime);Text("ACTIUM",fontSize=30.sp,fontWeight=FontWeight.Black)
                Text("Tu espacio de evolución",fontSize=20.sp)
                Text("Configura el mismo proyecto Supabase que utilizas en la web y sincroniza Gradle para empezar.",color=Soft,lineHeight=22.sp)
            }}
        } else if(!logged) {
            AuthScreen(busy,error,notice){email,password,signup->
                if(!busy){busy=true;error="";notice="";scope.launch{
                    try{notice=withContext(Dispatchers.IO){api.auth(email,password,signup)}
                        if(api.token!=null){logged=true;page="Inicio";notice="";reload()}}
                    catch(e:CancellationException){throw e}
                    catch(e:Exception){error=e.message?:"No se pudo iniciar sesión"}
                    finally{busy=false}
                }}
            }
        } else {
            AppFrame(page,LocalDate.parse(dateText),busy,error,notice,{page=it;notice=""},{dateText=it.toString()},::refresh,
                {api.token=null;api.userId=null;data=emptyMap();logged=false;error="";notice=""}){
                when(page) {
                    "Inicio"->Dashboard(data,LocalDate.parse(dateText)){page=it}
                    "Comidas"->FoodScreen(data,LocalDate.parse(dateText),save){page=it}
                    "Agua"->WaterScreen(data,LocalDate.parse(dateText),save){page=it}
                    "Sueño"->SleepScreen(data,LocalDate.parse(dateText),save){page=it}
                    "Entrenar"->TrainingScreen(data,LocalDate.parse(dateText),save)
                    "Medidas"->ProgressScreen(data,LocalDate.parse(dateText),save)
                    "Asistente"->CoachScreen(api)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppFrame(page:String,date:LocalDate,busy:Boolean,error:String,notice:String,onPage:(String)->Unit,onDate:(LocalDate)->Unit,onRefresh:()->Unit,onSignOut:()->Unit,content:@Composable ()->Unit) {
    var menu by remember{mutableStateOf(false)}
    val context=LocalContext.current
    Scaffold(containerColor=Ink,topBar={Column(Modifier.statusBarsPadding()){
        Row(Modifier.fillMaxWidth().padding(start=22.dp,end=10.dp,top=14.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Glyph("pulse",Lime,Modifier.size(25.dp));Text("ACTIUM",fontSize=21.sp,letterSpacing=2.sp,fontWeight=FontWeight.Black)
            }
            Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(onClick={android.app.DatePickerDialog(context,{_,year,month,day->onDate(LocalDate.of(year,month+1,day))},date.year,date.monthValue-1,date.dayOfMonth).apply{datePicker.maxDate=System.currentTimeMillis()}.show()}){
                    Text(if(date==LocalDate.now())"HOY" else date.format(DateTimeFormatter.ofPattern("dd MMM",Locale.forLanguageTag("es"))),color=Soft,fontSize=11.sp)
                }
                IconButton(onClick=onRefresh,enabled=!busy,modifier=Modifier.semantics{contentDescription="Actualizar registros"}){Glyph("refresh",if(busy)Border else Soft,Modifier.size(19.dp))}
            }
        }
        if(busy)LinearProgressIndicator(color=Lime,trackColor=Ink,modifier=Modifier.fillMaxWidth().height(2.dp))
    }},bottomBar={
        NavigationBar(containerColor=Ink,tonalElevation=0.dp){
            listOf(Triple("Inicio","Hoy","home"),Triple("Comidas","Diario","food"),Triple("Entrenar","Entrenar","gym"),Triple("Medidas","Progreso","chart"),Triple("Más","Más","more")).forEach{(target,label,icon)->
                val selected=page==target||(target=="Comidas"&&page in listOf("Agua","Sueño"))||(target=="Más"&&page=="Asistente")
                NavigationBarItem(selected=selected,onClick={if(target=="Más")menu=true else onPage(target)},icon={Glyph(icon,if(selected)Lime else Soft)},label={Text(label,fontSize=10.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)},colors=NavigationBarItemDefaults.colors(indicatorColor=Lime.copy(alpha=.1f),selectedTextColor=Lime,unselectedTextColor=Soft))
            }
        }
    }){padding->Column(Modifier.fillMaxSize().padding(padding)){
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp,lineHeight=18.sp,modifier=Modifier.padding(horizontal=22.dp,vertical=8.dp))
        if(notice.isNotBlank())Text(notice,color=Lime,fontSize=12.sp,modifier=Modifier.padding(horizontal=22.dp,vertical=6.dp))
        Box(Modifier.weight(1f)){content()}
    }}
    if(menu)ModalBottomSheet(onDismissRequest={menu=false},containerColor=Surface){Column(Modifier.padding(horizontal=24.dp).padding(bottom=30.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Eyebrow("Todo tu progreso");Text("Tu espacio ACTIUM",fontSize=25.sp,fontWeight=FontWeight.Bold)
        listOf(Triple("Agua","Hidratación","water"),Triple("Sueño","Sueño y recuperación","moon"),Triple("Asistente","Asistente personal","spark")).forEach{(target,label,icon)->
            TextButton(onClick={menu=false;onPage(target)},modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
                Glyph(icon,Lime);Spacer(Modifier.width(14.dp));Text(label,color=White,modifier=Modifier.weight(1f));Glyph("arrow",Soft)
            }
        }
        HorizontalDivider(color=Border);Text("Tus registros se guardan en tu cuenta Supabase. Las metas se editan desde la web.",color=Soft,fontSize=12.sp,lineHeight=19.sp)
        TextButton(onClick={menu=false;onSignOut()},enabled=!busy){Text("Cerrar sesión",color=Soft)}
    }}
}

@Composable private fun AuthScreen(busy:Boolean,error:String,notice:String,submit:(String,String,Boolean)->Unit) {
    var email by rememberSaveable{mutableStateOf("")};var password by rememberSaveable{mutableStateOf("")};var signup by rememberSaveable{mutableStateOf(false)}
    Column(Modifier.fillMaxSize().background(Ink).imePadding().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(36.dp));Glyph("pulse",Lime,Modifier.size(44.dp));Eyebrow("BUILD. TRACK. EVOLVE.",Lime)
        Text("Tu próxima\nversión empieza\naquí.",fontSize=39.sp,fontWeight=FontWeight.Bold,lineHeight=43.sp,letterSpacing=(-1.5).sp)
        Text("Nutrición, fuerza y recuperación.\nUn solo lugar para tu evolución.",color=Soft,fontSize=14.sp,lineHeight=23.sp)
        Spacer(Modifier.height(8.dp))
        Text(if(signup)"Crea tu cuenta" else "Bienvenido de nuevo",fontSize=20.sp,fontWeight=FontWeight.SemiBold)
        OutlinedTextField(email,{email=it},label={Text("Correo electrónico")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth())
        OutlinedTextField(password,{password=it},label={Text("Contraseña")},singleLine=true,visualTransformation=PasswordVisualTransformation(),shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth())
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
        if(notice.isNotBlank())Text(notice,color=Lime,fontSize=12.sp)
        ActionButton(if(busy)"Conectando…" else if(signup)"Crear cuenta" else "Entrar a ACTIUM",{submit(email.trim(),password,signup)},enabled=email.contains('@')&&password.length>=6,modifier=Modifier.fillMaxWidth(),icon="arrow")
        TextButton(onClick={signup=!signup},enabled=!busy,modifier=Modifier.align(Alignment.CenterHorizontally)){Text(if(signup)"Ya tengo una cuenta" else "¿Primera vez? Crear cuenta",color=Soft)}
    }
}
