package com.actium.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun decimal(value:String)=value.replace(',','.').toDoubleOrNull()?.takeIf{it.isFinite()}
private fun valid(value:String,min:Double,max:Double=1e6)=decimal(value)?.let{it in min..max}==true

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FormSheet(title:String,description:String,dismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
    val saving=LocalSaving.current
    ModalBottomSheet(onDismissRequest={if(!saving)dismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=Surface){
        Column(Modifier.fillMaxWidth().heightIn(max=720.dp).imePadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Text(title,fontSize=26.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.7).sp)
            Text(description,fontSize=12.sp,lineHeight=19.sp,color=Soft)
            content()
        }
    }
}

@Composable fun NumberField(label:String,value:String,modifier:Modifier=Modifier,onChange:(String)->Unit){
    OutlinedTextField(value,{if(it.matches(Regex("[0-9]{0,6}([.,][0-9]{0,2})?")))onChange(it)},label={Text(label,fontSize=12.sp)},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),shape=RoundedCornerShape(13.dp),modifier=modifier,enabled=!LocalSaving.current)
}

@Composable fun MealForm(date:LocalDate,save:SaveRecord,dismiss:()->Unit){
    var name by rememberSaveable{mutableStateOf("")};var grams by rememberSaveable{mutableStateOf("100")}
    var kcal by rememberSaveable{mutableStateOf("")};var protein by rememberSaveable{mutableStateOf("")};var carbs by rememberSaveable{mutableStateOf("")};var fat by rememberSaveable{mutableStateOf("")}
    var type by rememberSaveable{mutableStateOf("breakfast")};var per100 by rememberSaveable{mutableStateOf(false)}
    val factor=if(per100)(decimal(grams)?:0.0)/100.0 else 1.0
    val ready=name.isNotBlank()&&valid(grams,.01)&&listOf(kcal,protein,carbs,fat).all{valid(it,0.0)}
    FormSheet("Añadir alimento","Elige el momento y registra los nutrientes de tu etiqueta o de la porción completa.",dismiss){
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("breakfast" to "Desayuno","lunch" to "Almuerzo","snack" to "Merienda","dinner" to "Cena","other" to "Otro").forEach{(key,label)->FilterChip(type==key,{type=key},label={Text(label,fontSize=11.sp)})}}
        OutlinedTextField(name,{name=it},label={Text("Alimento o plato")},placeholder={Text("Ej. omelette casero")},singleLine=true,shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth(),enabled=!LocalSaving.current)
        NumberField("Cantidad consumida (g)",grams,Modifier.fillMaxWidth()){grams=it}
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){FilterChip(!per100,{per100=false},label={Text("Porción completa",fontSize=11.sp)});FilterChip(per100,{per100=true},label={Text("Por cada 100 g",fontSize=11.sp)})}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){NumberField("Calorías (kcal)",kcal,Modifier.weight(1f)){kcal=it};NumberField("Proteína (g)",protein,Modifier.weight(1f)){protein=it}}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){NumberField("Carbos (g)",carbs,Modifier.weight(1f)){carbs=it};NumberField("Grasas (g)",fat,Modifier.weight(1f)){fat=it}}
        if(per100&&ready)Pill("Tu porción · ${number(decimal(kcal)!!*factor)} kcal")
        ActionButton("Guardar alimento",{save("meal_bundle",JSONObject().put("p_type",type).put("p_name",name.trim()).put("p_grams",decimal(grams)).put("p_kcal",decimal(kcal)!!*factor).put("p_protein",decimal(protein)!!*factor).put("p_carbs",decimal(carbs)!!*factor).put("p_fat",decimal(fat)!!*factor).put("p_eaten_at",stamp(date)),dismiss)},enabled=ready,modifier=Modifier.fillMaxWidth())
        Text("Introduce 0 si un nutriente no está presente. Los valores son los que tú registras, no estimaciones por foto.",color=Soft,fontSize=10.sp,lineHeight=17.sp)
    }
}

@Composable fun MeasurementForm(date:LocalDate,save:SaveRecord,dismiss:()->Unit){
    var weight by rememberSaveable{mutableStateOf("")};var waist by rememberSaveable{mutableStateOf("")};var chest by rememberSaveable{mutableStateOf("")};var arm by rememberSaveable{mutableStateOf("")};var thigh by rememberSaveable{mutableStateOf("")}
    val fields=listOf(Triple("weight_kg",weight,20.0 to 500.0),Triple("waist_cm",waist,20.0 to 300.0),Triple("chest_cm",chest,20.0 to 300.0),Triple("arm_cm",arm,10.0 to 150.0),Triple("thigh_cm",thigh,10.0 to 200.0))
    val ready=fields.any{it.second.isNotBlank()}&&fields.all{it.second.isBlank()||valid(it.second,it.third.first,it.third.second)}
    FormSheet("Una nueva referencia","Completa las medidas que tengas. No necesitas rellenarlas todas.",dismiss){
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){NumberField("Peso (kg)",weight,Modifier.weight(1f)){weight=it};NumberField("Cintura (cm)",waist,Modifier.weight(1f)){waist=it}}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){NumberField("Pecho (cm)",chest,Modifier.weight(1f)){chest=it};NumberField("Brazo (cm)",arm,Modifier.weight(1f)){arm=it}}
        NumberField("Muslo (cm)",thigh,Modifier.fillMaxWidth()){thigh=it}
        Text("Peso 20–500 kg · cintura y pecho 20–300 cm · brazo 10–150 cm · muslo 10–200 cm.",fontSize=10.sp,color=Soft,lineHeight=17.sp)
        ActionButton("Guardar medición",{val record=JSONObject().put("measured_at",stamp(date));fields.filter{it.second.isNotBlank()}.forEach{record.put(it.first,decimal(it.second))};save("body_measurements",record,dismiss)},enabled=ready,modifier=Modifier.fillMaxWidth(),icon="scale")
    }
}

@Composable private fun DateTimeControl(label:String,value:LocalDateTime,onChange:(LocalDateTime)->Unit){
    val context=LocalContext.current
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text(label,fontSize=12.sp,color=Soft)
        OutlinedButton(enabled=!LocalSaving.current,onClick={
            android.app.DatePickerDialog(context,{_,year,month,day->
                android.app.TimePickerDialog(context,{_,hour,minute->onChange(LocalDateTime.of(year,month+1,day,hour,minute))},value.hour,value.minute,true).show()
            },value.year,value.monthValue-1,value.dayOfMonth).apply{datePicker.maxDate=System.currentTimeMillis()}.show()
        },shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(16.dp)){
            Text(value.format(DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm",Locale.forLanguageTag("es"))),color=White,modifier=Modifier.weight(1f));Glyph("timer",Purple)
        }
    }
}

@Composable fun SleepForm(date:LocalDate,save:SaveRecord,dismiss:()->Unit){
    var startText by rememberSaveable{mutableStateOf(date.minusDays(1).atTime(22,30).toString())};var endText by rememberSaveable{mutableStateOf(date.atTime(7,0).toString())};var quality by rememberSaveable{mutableIntStateOf(4)}
    val start=LocalDateTime.parse(startText);val end=LocalDateTime.parse(endText)
    val startInstant=start.atZone(ZoneId.systemDefault()).toInstant();val endInstant=end.atZone(ZoneId.systemDefault()).toInstant()
    val minutes=Duration.between(startInstant,endInstant).toMinutes()
    FormSheet("Registra tu descanso","Selecciona las fechas y horas con el calendario y el reloj.",dismiss){
        DateTimeControl("Me dormí",start){startText=it.toString()};DateTimeControl("Desperté",end){endText=it.toString()}
        if(minutes in 1..1440)Pill("Duración · ${minutes/60} h ${minutes%60} min",Purple)else Text("El despertar debe ser posterior al inicio, con una duración máxima de 24 horas.",color=MaterialTheme.colorScheme.error,fontSize=11.sp)
        Text("¿Cómo descansaste?",fontSize=13.sp,color=Soft)
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){(1..5).forEach{n->FilterChip(quality==n,{quality=n},label={Text("$n",fontSize=13.sp)})}}
        Text("1 · Poco reparador     5 · Muy reparador",color=Soft,fontSize=10.sp)
        ActionButton("Guardar descanso",{save("sleep_entries",JSONObject().put("slept_at",startInstant.toString()).put("woke_at",endInstant.toString()).put("quality",quality),dismiss)},enabled=minutes in 1..1440,modifier=Modifier.fillMaxWidth(),icon="moon")
    }
}

@Composable fun ExerciseForm(save:SaveRecord,dismiss:()->Unit){
    var name by rememberSaveable{mutableStateOf("")};var muscle by rememberSaveable{mutableStateOf("Pecho")}
    FormSheet("Tu biblioteca de fuerza","Añade un ejercicio para reconocerlo en tus sesiones y estadísticas.",dismiss){
        OutlinedTextField(name,{name=it},label={Text("Nombre del ejercicio")},placeholder={Text("Ej. press de banca")},singleLine=true,shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth(),enabled=!LocalSaving.current)
        Text("Grupo muscular",fontSize=12.sp,color=Soft)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("Pecho","Espalda","Hombros","Bíceps","Tríceps","Cuádriceps","Femoral","Glúteos","Pantorrillas","Abdomen","Cardio").forEach{group->FilterChip(muscle==group,{muscle=group},label={Text(group,fontSize=11.sp)})}}
        ActionButton("Guardar ejercicio",{save("exercises",JSONObject().put("name",name.trim()).put("muscle_group",muscle),dismiss)},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth(),icon="gym")
    }
}
