package com.actium.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish=Locale.forLanguageTag("es")
fun Records.rows(table:String)=get(table).orEmpty()
fun Records.onDay(table:String,column:String,date:LocalDate)=rows(table).filter{dayOf(it.optString(column))==date}
fun Records.goals(date:LocalDate)=rows("daily_goals").filter{it.optString("effective_from")<=date.toString()}.maxByOrNull{it.optString("effective_from")}
fun Records.foodItems(date:LocalDate):List<JSONObject>{val ids=onDay("meals","eaten_at",date).map{it.optString("id")}.toSet();return rows("meal_items").filter{it.optString("meal_id") in ids}}
fun List<JSONObject>.sum(key:String)=sumOf{it.optDouble(key,0.0).takeIf{v->v.isFinite()}?:0.0}
fun Records.points(key:String)=rows("body_measurements").mapNotNull{r->val date=dayOf(r.optString("measured_at"));val v=r.optDouble(key,Double.NaN);if(date!=null&&v.isFinite()&&v>0)ChartPoint(date,v)else null}
fun stamp(date:LocalDate)=date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toString()
fun timeLabel(value:String)=runCatching{OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM · HH:mm",spanish))}.getOrDefault("Sin fecha")
private fun week(date:LocalDate,calc:(LocalDate)->Double?)=(6 downTo 0).map{val d=date.minusDays(it.toLong());d.format(DateTimeFormatter.ofPattern("EEEEE",spanish)).uppercase(spanish) to calc(d)}
private val labels=mapOf("breakfast" to "Desayuno","lunch" to "Almuerzo","snack" to "Merienda","dinner" to "Cena","other" to "Otro")

@Composable fun PageIntro(eyebrow:String,title:String,subtitle:String?=null){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Eyebrow(eyebrow);Text(title,fontSize=30.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.9).sp,lineHeight=35.sp);if(subtitle!=null)Text(subtitle,color=Soft,fontSize=12.sp,lineHeight=19.sp)}
}

@Composable fun Dashboard(data:Records,date:LocalDate,navigate:(String)->Unit){
    val foods=data.foodItems(date);val goals=data.goals(date)
    val kcal=foods.sum("kcal");val target=goals?.optDouble("calories",2300.0)?:2300.0
    val water=data.onDay("water_entries","consumed_at",date).sum("amount_ml")
    val waterGoal=goals?.optDouble("water_ml",2500.0)?:2500.0
    val sleep=data.onDay("sleep_entries","woke_at",date)
    val hours=sleep.sumOf{runCatching{Duration.between(OffsetDateTime.parse(it.optString("slept_at")),OffsetDateTime.parse(it.optString("woke_at"))).toMinutes()/60.0}.getOrDefault(0.0)}
    val weights=dailyMeans(data.points("weight_kg"),date.minusDays(29),date)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=14.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("TU CENTRO DE CONTROL","Cada hábito cuenta.",date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM",spanish)).replaceFirstChar{it.uppercase()})}
        item{Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(0xFF263522),androidx.compose.ui.graphics.Color(0xFF19231D))),RoundedCornerShape(25.dp)).border(1.dp,Lime.copy(alpha=.16f),RoundedCornerShape(25.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("ENERGÍA DE HOY",Lime);Pill("RECOMPOSICIÓN")}
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(9.dp)){
                    Text(if(kcal<=target)"Te quedan" else "Sobre la meta",fontSize=13.sp,color=Soft)
                    Text(number(kotlin.math.abs(target-kcal)),fontSize=33.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                    Text("kcal de tu objetivo",fontSize=11.sp,color=Soft)
                    if(foods.isEmpty())Text("Aún sin alimentos\nregistrados",fontSize=10.sp,color=Lime.copy(alpha=.8f),lineHeight=16.sp)
                }
                EnergyRing(kcal,target,Modifier.size(144.dp))
            }
        }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            MetricBar("Proteína",foods.sum("protein_g"),goals?.optDouble("protein_g",150.0)?:150.0,Purple,Modifier.weight(1f))
            MetricBar("Carbos",foods.sum("carbs_g"),goals?.optDouble("carbs_g",279.0)?:279.0,Amber,Modifier.weight(1f))
            MetricBar("Grasas",foods.sum("fat_g"),goals?.optDouble("fat_g",65.0)?:65.0,Blue,Modifier.weight(1f))
        }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){ActionButton("Registrar comida",{navigate("Comidas")},modifier=Modifier.weight(1f));OutlinedButton(onClick={navigate("Agua")},shape=RoundedCornerShape(14.dp),modifier=Modifier.height(50.dp)){Glyph("water",Blue);Spacer(Modifier.width(7.dp));Text("Agua",color=Blue,fontSize=12.sp)}}}
        item{Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Tu semana",fontSize=17.sp,fontWeight=FontWeight.SemiBold);Pill("KCAL REGISTRADAS",Soft)}
            WeekBars(week(date){d->data.foodItems(d).takeIf{it.isNotEmpty()}?.sum("kcal")},"kcal")
        }}
        item{Tile{SectionTitle("Evolución del peso","Ver todo"){navigate("Medidas")}
            if(weights.isNotEmpty()){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Bottom){Row(verticalAlignment=Alignment.Bottom){Text(number(weights.last().value,1),fontSize=31.sp,fontWeight=FontWeight.Bold);Text(" kg",color=Soft,fontSize=13.sp)};Pill("30 DÍAS",Soft)}}
            LineGraph(weights,"kg")
            weeklyMean(data.points("weight_kg"),date)?.let{Text("Promedio de 7 días · ${number(it,1)} kg",fontSize=11.sp,color=Soft)}
        }}
        item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Tile(Modifier.weight(1f)){Glyph("water",Blue);Text("Hidratación",color=Soft,fontSize=11.sp);Text("${number(water/1000,1)} L",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Blue);LinearProgressIndicator(progress={progressFraction(water,waterGoal)},color=Blue,trackColor=Blue.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().height(4.dp));Text("Meta ${number(waterGoal/1000,1)} L",fontSize=10.sp,color=Soft)}
            Tile(Modifier.weight(1f)){Glyph("moon",Purple);Text("Descanso",color=Soft,fontSize=11.sp);Text(if(sleep.isEmpty())"—" else "${number(hours,1)} h",fontSize=26.sp,fontWeight=FontWeight.Bold,color=Purple);Text(if(sleep.isEmpty())"Sin registrar" else "Al despertar",fontSize=10.sp,color=Soft);TextButton(onClick={navigate("Sueño")},contentPadding=PaddingValues(0.dp)){Text("Ver descanso →",color=Purple,fontSize=10.sp)}}
        }}
        item{Tile{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){Glyph("gym",Lime,Modifier.size(32.dp));Column(Modifier.weight(1f)){Text("Haz que cada serie cuente",fontSize=14.sp,fontWeight=FontWeight.SemiBold);Text("${data.onDay("workout_sessions","started_at",date).size} sesiones registradas este día",fontSize=11.sp,color=Soft)};IconButton(onClick={navigate("Entrenar")}){Glyph("arrow",Lime)}}}}
    }
}

@Composable fun DiaryTabs(selected:String,navigate:(String)->Unit){Row(horizontalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.horizontalScroll(rememberScrollState())){listOf("Comidas","Agua","Sueño").forEach{label->FilterChip(selected=selected==label,onClick={navigate(label)},label={Text(label,fontSize=12.sp)},shape=RoundedCornerShape(12.dp))}}}

@Composable fun FoodScreen(data:Records,date:LocalDate,save:SaveRecord,navigate:(String)->Unit){
    var add by rememberSaveable{mutableStateOf(false)}
    val foods=data.foodItems(date);val meals=data.onDay("meals","eaten_at",date)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("NUTRICIÓN CON CONTEXTO","Tu diario.","Registra lo que realmente consumiste.")}
        item{DiaryTabs("Comidas",navigate)}
        item{Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Eyebrow("TOTAL REGISTRADO");Text("${number(foods.sum("kcal"))} kcal",fontSize=29.sp,fontWeight=FontWeight.Bold)};Glyph("food",Lime,Modifier.size(32.dp))};Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Text("P ${number(foods.sum("protein_g"))} g",color=Purple,fontSize=12.sp);Text("C ${number(foods.sum("carbs_g"))} g",color=Amber,fontSize=12.sp);Text("G ${number(foods.sum("fat_g"))} g",color=Blue,fontSize=12.sp)};ActionButton("Añadir alimento",{add=true},modifier=Modifier.fillMaxWidth())}}
        labels.forEach{(type,label)->item(key=type){Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,fontSize=16.sp,fontWeight=FontWeight.SemiBold);val ids=meals.filter{it.optString("meal_type")==type}.map{it.optString("id")};val items=foods.filter{it.optString("meal_id") in ids};Text(if(items.isEmpty())"Sin registrar" else "${number(items.sum("kcal"))} kcal",color=Soft,fontSize=11.sp)}
            val entries=meals.filter{it.optString("meal_type")==type}
            if(entries.isEmpty())Text("Aquí aparecerán tus alimentos.",color=Soft,fontSize=12.sp)
            entries.forEach{m->foods.filter{it.optString("meal_id")==m.optString("id")}.forEach{f->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(f.optString("food_name"),fontSize=13.sp,fontWeight=FontWeight.Medium);Text("${number(f.optDouble("grams"),1)} g · P ${number(f.optDouble("protein_g"))} / C ${number(f.optDouble("carbs_g"))} / G ${number(f.optDouble("fat_g"))}",fontSize=10.sp,color=Soft)};Text("${number(f.optDouble("kcal"))}",fontSize=14.sp,color=Lime)}};if(!m.isNull("notes"))Text(m.optString("notes"),color=Soft,fontSize=11.sp)}
        }}}
    }
    if(add)MealForm(date,save){add=false}
}

@Composable fun WaterScreen(data:Records,date:LocalDate,save:SaveRecord,navigate:(String)->Unit){
    val entries=data.onDay("water_entries","consumed_at",date).sortedByDescending{it.optString("consumed_at")}
    val total=entries.sum("amount_ml");val goal=data.goals(date)?.optDouble("water_ml",2500.0)?:2500.0
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("HÁBITOS QUE SUMAN","Mantén el ritmo.")};item{DiaryTabs("Agua",navigate)}
        item{Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Glyph("water",Blue,Modifier.size(35.dp));Pill("META EDITABLE",Blue)};Text("${number(total/1000,2)} L",fontSize=46.sp,fontWeight=FontWeight.Bold,color=Blue,letterSpacing=(-2).sp);Text("de ${number(goal/1000,2)} L registrados",color=Soft,fontSize=13.sp);LinearProgressIndicator(progress={progressFraction(total,goal)},color=Blue,trackColor=Blue.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().height(9.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(150,250,500).forEach{ml->OutlinedButton(enabled=!LocalSaving.current,onClick={save("water_entries",JSONObject().put("amount_ml",ml).put("consumed_at",stamp(date)),{})},shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(9.dp),modifier=Modifier.weight(1f)){Text("+ $ml ml",fontSize=11.sp,color=Blue)}}}}}
        item{Tile{SectionTitle("Últimos 7 días");WeekBars(week(date){d->data.onDay("water_entries","consumed_at",d).takeIf{it.isNotEmpty()}?.sum("amount_ml")},"ml",Blue)}}
        item{SectionTitle("Vasos y botellas")}
        if(entries.isEmpty())item{EmptyChart("Un vaso a la vez","Pulsa una cantidad para registrar tu agua.")}
        items(entries,key={it.optString("id")}){r->Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${r.optInt("amount_ml")} ml",fontWeight=FontWeight.SemiBold,color=Blue);Text(timeLabel(r.optString("consumed_at")),color=Soft,fontSize=11.sp)}}}
    }
}

@Composable fun SleepScreen(data:Records,date:LocalDate,save:SaveRecord,navigate:(String)->Unit){
    var add by rememberSaveable{mutableStateOf(false)}
    val entries=data.rows("sleep_entries").filter{dayOf(it.optString("woke_at"))?.let{d->!d.isAfter(date)}==true}.sortedByDescending{it.optString("woke_at")}
    fun hours(r:JSONObject)=runCatching{Duration.between(OffsetDateTime.parse(r.optString("slept_at")),OffsetDateTime.parse(r.optString("woke_at"))).toMinutes()/60.0}.getOrDefault(0.0)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("RECUPERACIÓN","Descansar también suma.")};item{DiaryTabs("Sueño",navigate)}
        item{Tile{Glyph("moon",Purple,Modifier.size(36.dp));Eyebrow("ÚLTIMO DESCANSO");Text(entries.firstOrNull()?.let{"${number(hours(it),1)} h"}?:"Sin registros",fontSize=36.sp,fontWeight=FontWeight.Bold,color=Purple);Text(entries.firstOrNull()?.let{timeLabel(it.optString("woke_at"))+" · Calidad ${it.optInt("quality")}/5"}?:"Registra tus horas para ver tu recuperación.",fontSize=12.sp,color=Soft);ActionButton("Registrar descanso",{add=true},modifier=Modifier.fillMaxWidth(),icon="moon")}}
        item{Tile{SectionTitle("Tus noches esta semana");WeekBars(week(date){d->data.onDay("sleep_entries","woke_at",d).takeIf{it.isNotEmpty()}?.sumOf{hours(it)}},"h",Purple)}}
        item{SectionTitle("Historial de descanso")}
        items(entries.take(30),key={it.optString("id")}){r->Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("${number(hours(r),1)} horas",fontWeight=FontWeight.SemiBold,color=Purple);Text(timeLabel(r.optString("woke_at")),fontSize=11.sp,color=Soft)};Pill("${r.optInt("quality")}/5",Purple)}}}
    }
    if(add)SleepForm(date,save){add=false}
}

@Composable fun ProgressScreen(data:Records,date:LocalDate,save:SaveRecord){
    var add by rememberSaveable{mutableStateOf(false)};var range by rememberSaveable{mutableIntStateOf(30)};var metric by rememberSaveable{mutableStateOf("weight_kg")}
    val all=data.points(metric);val points=dailyMeans(all,date.minusDays(range-1L),date);val unit=if(metric=="weight_kg")"kg" else "cm"
    val entries=data.rows("body_measurements").filter{dayOf(it.optString("measured_at"))?.let{d->!d.isAfter(date)}==true}.sortedByDescending{it.optString("measured_at")}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("MÁS QUE UN NÚMERO","Mira tu evolución.","Peso, cintura y constancia. Tu historia en datos.")}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(metric=="weight_kg",{metric="weight_kg"},label={Text("Peso")});FilterChip(metric=="waist_cm",{metric="waist_cm"},label={Text("Cintura")})}}
        item{Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Eyebrow(if(metric=="weight_kg")"PESO REGISTRADO" else "CINTURA REGISTRADA");Text(points.lastOrNull()?.let{"${number(it.value,1)} $unit"}?:"—",fontSize=36.sp,fontWeight=FontWeight.Bold)};Glyph("chart",Lime,Modifier.size(32.dp))}
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf(7,30,90).forEach{days->FilterChip(range==days,{range=days},label={Text("${days}D",fontSize=11.sp)})}}
            LineGraph(points,unit)
            if(points.size>1){val difference=points.last().value-points.first().value;Text("${if(difference>0)"+" else ""}${number(difference,1)} $unit entre el primer y último día del período",fontSize=11.sp,color=Soft,lineHeight=18.sp)}
            weeklyMean(all,date)?.let{Text("Promedio semanal · ${number(it,1)} $unit",fontSize=12.sp,color=Lime)}
            Text("Cada día medido tiene el mismo peso en el promedio. No se completan días sin datos.",fontSize=10.sp,color=Soft,lineHeight=16.sp)
        }}
        item{ActionButton("Añadir medición",{add=true},modifier=Modifier.fillMaxWidth(),icon="scale")}
        item{SectionTitle("Tus mediciones")}
        if(entries.isEmpty())item{EmptyChart("Tu punto de partida","Añade una medición real para empezar.")}
        items(entries.take(40),key={it.optString("id")}){r->Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(timeLabel(r.optString("measured_at")),fontSize=11.sp,color=Soft);Glyph("scale",Soft,Modifier.size(17.dp))};Row(horizontalArrangement=Arrangement.spacedBy(22.dp)){if(!r.isNull("weight_kg"))Column{Text("${number(r.optDouble("weight_kg"),1)} kg",fontSize=21.sp,fontWeight=FontWeight.SemiBold);Text("Peso",fontSize=10.sp,color=Soft)};if(!r.isNull("waist_cm"))Column{Text("${number(r.optDouble("waist_cm"),1)} cm",fontSize=21.sp,fontWeight=FontWeight.SemiBold);Text("Cintura",fontSize=10.sp,color=Soft)}};val extra=listOf("chest_cm" to "Pecho","arm_cm" to "Brazo","thigh_cm" to "Muslo").filter{!r.isNull(it.first)};if(extra.isNotEmpty())Text(extra.joinToString(" · "){(key,label)->"$label ${number(r.optDouble(key),1)} cm"},color=Soft,fontSize=11.sp)}}
    }
    if(add)MeasurementForm(date,save){add=false}
}

@Composable fun TrainingScreen(data:Records,date:LocalDate,save:SaveRecord){
    var add by rememberSaveable{mutableStateOf(false)}
    val sessions=data.rows("workout_sessions").filter{dayOf(it.optString("started_at"))?.let{d->!d.isAfter(date)}==true}
    val ids=sessions.map{it.optString("id")}.toSet();val sets=data.rows("workout_sets").filter{it.optString("session_id") in ids}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("FUERZA CON DIRECCIÓN","Cada serie cuenta.","Tus sesiones, volumen y mejores marcas.")}
        item{Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Eyebrow("VOLUMEN ACUMULADO");Text("${number(sets.sumOf{it.optDouble("weight_kg",0.0)*it.optInt("reps")})} kg",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Lime)};Glyph("gym",Lime,Modifier.size(34.dp))};Text("${sessions.size} sesiones · ${sets.size} series · peso × repeticiones",fontSize=12.sp,color=Soft)}}
        item{Tile{SectionTitle("Volumen de tu semana");WeekBars(week(date){d->val daySessions=sessions.filter{dayOf(it.optString("started_at"))==d};val dayIds=daySessions.map{it.optString("id")};if(daySessions.isEmpty())null else sets.filter{it.optString("session_id") in dayIds}.sumOf{it.optDouble("weight_kg",0.0)*it.optInt("reps")}},"kg")}}
        item{RestTimer()}
        item{SectionTitle("Tus ejercicios","Añadir"){add=true}}
        if(data.rows("exercises").isEmpty())item{EmptyChart("Construye tu biblioteca","Añade un ejercicio y registra tus series desde la web.")}
        items(data.rows("exercises"),key={it.optString("id")}){exercise->val exerciseSets=sets.filter{it.optString("exercise_id")==exercise.optString("id")};Tile{
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){Glyph("gym",Lime);Column(Modifier.weight(1f)){Text(exercise.optString("name"),fontSize=15.sp,fontWeight=FontWeight.SemiBold);Text(exercise.optString("muscle_group"),fontSize=11.sp,color=Soft)};if(exerciseSets.isNotEmpty())Pill("${number(exerciseSets.maxOf{it.optDouble("weight_kg",0.0)},1)} kg")}
            if(exerciseSets.isNotEmpty())Text("${exerciseSets.size} series registradas · peso máximo levantado",color=Soft,fontSize=11.sp)
        }}
        item{SectionTitle("Sesiones recientes")}
        items(sessions.sortedByDescending{it.optString("started_at")}.take(15),key={it.optString("id")}){r->Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(timeLabel(r.optString("started_at")),fontSize=12.sp);Pill(if(r.isNull("ended_at"))"EN CURSO" else "FINALIZADA",Soft)};Text("${sets.count{it.optString("session_id")==r.optString("id")}} series",color=Soft,fontSize=12.sp)}}
        item{Text("Las series y el plan semanal se registran desde la web y aparecen aquí al actualizar.",color=Soft,fontSize=11.sp,lineHeight=18.sp)}
    }
    if(add)ExerciseForm(save){add=false}
}

@Composable private fun RestTimer(){
    var remaining by rememberSaveable{mutableIntStateOf(90)};var end by remember{mutableStateOf<Long?>(null)}
    LaunchedEffect(end){val deadline=end?:return@LaunchedEffect;while(true){remaining=maxOf(0,((deadline-android.os.SystemClock.elapsedRealtime()+999)/1000).toInt());if(remaining==0){end=null;break};delay(250)}}
    Tile{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Eyebrow("DESCANSO ENTRE SERIES");Text("${(remaining/60).toString().padStart(2,'0')}:${(remaining%60).toString().padStart(2,'0')}",fontSize=38.sp,fontWeight=FontWeight.Bold,color=Lime)};Glyph("timer",Lime,Modifier.size(33.dp))};Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf(60,90,120).forEach{n->FilterChip(remaining==n&&end==null,{end=null;remaining=n},label={Text("${n}s",fontSize=11.sp)})}}
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){ActionButton(if(end==null)"Iniciar" else "Pausar",{end=if(end!=null)null else android.os.SystemClock.elapsedRealtime()+maxOf(1,remaining)*1000L},modifier=Modifier.weight(1f),icon="timer");OutlinedButton(onClick={end=null;remaining=90},shape=RoundedCornerShape(13.dp),modifier=Modifier.height(50.dp)){Text("Reiniciar",fontSize=12.sp,color=Soft)}}
        if(remaining==0)Text("Descanso terminado",color=Lime,fontSize=12.sp)
    }
}

@Composable internal fun CoachScreen(api:Api){
    val scope=rememberCoroutineScope();var consent by remember{mutableStateOf(false)};var question by rememberSaveable{mutableStateOf("")};var answer by rememberSaveable{mutableStateOf("")};var waiting by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().imePadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        item{PageIntro("TU ASISTENTE ACTIUM","Convierte datos\nen perspectiva.","Consulta tus registros de los últimos 14 días.")}
        item{Tile{Glyph("spark",Lime,Modifier.size(36.dp));Text("¿Qué quieres revisar?",fontSize=21.sp,fontWeight=FontWeight.SemiBold);listOf("Resume mis registros de esta semana","¿Cuánta proteína he registrado?","Compara mi peso y cintura").forEach{prompt->OutlinedButton(onClick={question=prompt},shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth()){Text(prompt,fontSize=11.sp,color=Soft)}}}}
        item{Tile{OutlinedTextField(question,{if(it.length<=700)question=it},label={Text("Tu pregunta")},minLines=3,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth());Row(verticalAlignment=Alignment.CenterVertically){Checkbox(consent,{consent=it});Text("Autorizo enviar un resumen de mis registros al proveedor de IA. No se envían fotos.",fontSize=11.sp,color=Soft,lineHeight=17.sp,modifier=Modifier.weight(1f))};ActionButton(if(waiting)"Analizando…" else "Consultar mis registros",{waiting=true;error="";scope.launch{try{answer=withContext(Dispatchers.IO){api.askCoach(question.trim())}}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message?:"No se pudo consultar"}finally{waiting=false}}},enabled=consent&&question.isNotBlank()&&!waiting,modifier=Modifier.fillMaxWidth(),icon="spark");if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp)}}
        if(answer.isNotBlank())item{Tile{Eyebrow("ASISTENTE ACTIUM",Lime);Text(answer,fontSize=14.sp,lineHeight=23.sp)}}
        item{Text("Requiere la función actium-coach configurada en Supabase. Las respuestas dependen de la información que hayas registrado.",color=Soft,fontSize=11.sp,lineHeight=18.sp)}
    }
}
