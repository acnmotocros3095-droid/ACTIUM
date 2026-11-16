package com.actium.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import java.util.Locale

val Ink = Color(0xFF0D1115)
val Surface = Color(0xFF191F26)
val Border = Color(0xFF2A333C)
val Lime = Color(0xFFC7FF45)
val Soft = Color(0xFF94A3AF)
val White = Color(0xFFF2F5F7)
val Purple = Color(0xFFB4A2FF)
val Blue = Color(0xFF77C9F5)
val Amber = Color(0xFFF2BF79)
val LocalSaving = staticCompositionLocalOf { false }
fun number(value: Double, digits: Int = 0): String = String.format(Locale.forLanguageTag("es-BO"), "%,.${digits}f", value)

@Composable fun ActiumTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Lime, onPrimary = Ink, background = Ink,
        surface = Surface, onSurface = White, onBackground = White, secondary = Purple,
        surfaceVariant = Border, onSurfaceVariant = Soft, outline = Border)) {
        androidx.compose.material3.Surface(color=Ink,contentColor=White,modifier=Modifier.fillMaxSize()){content()}
    }
}

@Composable fun Glyph(name: String, color: Color = Soft, modifier: Modifier = Modifier.size(22.dp)) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val p = Path()
            fun line(vararg xy: Float) { p.moveTo(xy[0],xy[1]); for(i in 2 until xy.size step 2)p.lineTo(xy[i],xy[i+1]) }
            when(name) {
                "home" -> { line(3f,10f,12f,3f,21f,10f); line(5f,9f,5f,21f,10f,21f,10f,14f,14f,14f,14f,21f,19f,21f,19f,9f) }
                "food" -> { line(5f,3f,5f,10f,9f,10f,9f,3f);line(7f,3f,7f,21f);line(18f,3f,15f,11f,18f,11f,18f,21f) }
                "gym" -> { line(4f,9f,4f,15f);line(7f,6f,7f,18f);line(17f,6f,17f,18f);line(20f,9f,20f,15f);line(7f,12f,17f,12f) }
                "chart" -> { line(4f,4f,4f,20f,21f,20f);line(7f,15f,11f,10f,15f,13f,21f,5f) }
                "water" -> { p.moveTo(12f,2f);p.cubicTo(9f,7f,5f,11f,5f,15f);p.cubicTo(5f,24f,19f,24f,19f,15f);p.cubicTo(19f,11f,15f,7f,12f,2f);p.close() }
                "moon" -> { p.moveTo(19f,15f);p.cubicTo(8f,18f,5f,8f,11f,3f);p.cubicTo(-1f,5f,0f,22f,12f,22f);p.cubicTo(16f,22f,19f,19f,19f,15f) }
                "plus" -> { line(12f,5f,12f,19f);line(5f,12f,19f,12f) }
                "arrow" -> {line(5f,12f,19f,12f);line(14f,7f,19f,12f,14f,17f)}
                "refresh" -> {p.moveTo(19f,7f);p.cubicTo(10f,-3f,-2f,10f,6f,18f);p.cubicTo(12f,24f,21f,19f,21f,12f);line(19f,2f,19f,7f,14f,7f)}
                "spark" -> {line(12f,2f,15f,9f,22f,12f,15f,15f,12f,22f,9f,15f,2f,12f,9f,9f,12f,2f)}
                "close" -> {line(6f,6f,18f,18f);line(6f,18f,18f,6f)}
                "scale" -> {line(4f,5f,20f,5f,20f,21f,4f,21f,4f,5f);line(8f,9f,16f,9f);line(12f,9f,14f,6f)}
                "timer" -> {drawCircle(color,8f,Offset(12f,14f),style=Stroke(1.7f));line(9f,2f,15f,2f);line(12f,6f,12f,2f);line(12f,9f,12f,14f,16f,14f)}
                "more" -> {for(x in listOf(5f,12f,19f))drawCircle(color,1.7f,Offset(x,12f))}
                else -> {line(2f,12f,6f,12f,9f,3f,14f,21f,18f,12f,22f,12f)}
            }
            drawPath(p,color,style=Stroke(1.7f,cap=StrokeCap.Round,join=StrokeJoin.Round))
        }
    }
}

@Composable fun Eyebrow(text: String, color: Color = Soft) {
    Text(text.uppercase(Locale.forLanguageTag("es")),color=color,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.8.sp)
}

@Composable fun Tile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Surface,RoundedCornerShape(22.dp))
        .border(1.dp,Border.copy(alpha=.65f),RoundedCornerShape(22.dp)).padding(20.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
}

@Composable fun SectionTitle(title: String, action: String? = null, onClick: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
        Text(title,fontSize=18.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
        if(action!=null)TextButton(onClick=onClick){Text(action,fontSize=12.sp,color=Lime)}
    }
}

@Composable fun Pill(text: String, color: Color = Lime) {
    Text(text,color=color,fontSize=10.sp,fontWeight=FontWeight.Medium,
        modifier=Modifier.background(color.copy(alpha=.09f),RoundedCornerShape(7.dp)).padding(horizontal=9.dp,vertical=6.dp))
}

@Composable fun ActionButton(text:String,onClick:()->Unit,enabled:Boolean=true,modifier:Modifier=Modifier,icon:String?="plus") {
    Button(onClick=onClick,enabled=enabled&&!LocalSaving.current,shape=RoundedCornerShape(14.dp),
        contentPadding=PaddingValues(horizontal=18.dp,vertical=15.dp),modifier=modifier.heightIn(min=50.dp)) {
        if(icon!=null){Glyph(icon,Ink,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))}
        Text(text,fontSize=13.sp,fontWeight=FontWeight.Bold)
    }
}

@Composable fun MetricBar(label:String,value:Double,target:Double,color:Color,modifier:Modifier=Modifier) {
    val fraction by animateFloatAsState(progressFraction(value,target),label="macro")
    Column(modifier.background(color.copy(alpha=.06f),RoundedCornerShape(17.dp)).padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text(label,color=Soft,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Row(verticalAlignment=Alignment.Bottom){Text(number(value),fontSize=23.sp,fontWeight=FontWeight.Bold,color=color);Text(" g",fontSize=11.sp,color=Soft)}
        LinearProgressIndicator(progress={fraction},color=color,trackColor=color.copy(alpha=.1f),modifier=Modifier.fillMaxWidth().height(4.dp))
        Text("de ${number(target)} g",fontSize=10.sp,color=Soft)
    }
}

@Composable fun EnergyRing(value:Double,target:Double,modifier:Modifier=Modifier) {
    val fraction by animateFloatAsState(progressFraction(value,target),label="energy")
    Box(modifier.size(164.dp).semantics { contentDescription="${number(value)} de ${number(target)} kilocalorías registradas" },contentAlignment=Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(9.dp)) {
            drawArc(Lime.copy(alpha=.09f),-90f,360f,false,style=Stroke(11.dp.toPx(),cap=StrokeCap.Round))
            if(fraction>0)drawArc(Brush.sweepGradient(listOf(Lime,Lime.copy(alpha=.5f),Lime)),-90f,360f*fraction,false,style=Stroke(11.dp.toPx(),cap=StrokeCap.Round))
        }
        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(number(value),fontSize=31.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp);Text("kcal registradas",fontSize=10.sp,color=Soft);Spacer(Modifier.height(7.dp));Text("de ${number(target)}",fontSize=10.sp,color=Lime)}
    }
}

@Composable fun EmptyChart(title:String,description:String) {
    Column(Modifier.fillMaxWidth().padding(vertical=25.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(50.dp).background(Lime.copy(alpha=.07f),CircleShape),contentAlignment=Alignment.Center){Glyph("chart",Lime)}
        Text(title,fontSize=14.sp,fontWeight=FontWeight.Medium)
        Text(description,fontSize=12.sp,color=Soft,lineHeight=18.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable fun LineGraph(points:List<ChartPoint>,unit:String,color:Color=Lime) {
    val domain=chartDomain(points)
    val summary=points.joinToString { "${it.date}: ${number(it.value,1)} $unit" }
    if(points.size<2){EmptyChart("Tu evolución empieza aquí","Registra al menos dos días para ver la tendencia.");return}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${number(domain.second,1)} $unit",fontSize=10.sp,color=Soft);Text("${number(domain.first,1)} $unit",fontSize=10.sp,color=Soft)}
        Canvas(Modifier.fillMaxWidth().height(140.dp).semantics{contentDescription=summary}) {
            val inset=7.dp.toPx();val width=size.width-2*inset;val height=size.height-2*inset
            for(i in 0..3){val y=inset+height*i/3;drawLine(Border.copy(alpha=.6f),Offset(inset,y),Offset(size.width-inset,y),1f,pathEffect=PathEffect.dashPathEffect(floatArrayOf(7f,8f)))}
            val span=maxOf(1,java.time.temporal.ChronoUnit.DAYS.between(points.first().date,points.last().date).toInt())
            val coordinates=points.map{Offset(inset+java.time.temporal.ChronoUnit.DAYS.between(points.first().date,it.date).toFloat()/span*width,inset+(1-(it.value-domain.first)/(domain.second-domain.first)).toFloat()*height)}
            val line=Path().apply{coordinates.forEachIndexed{i,o->if(i==0)moveTo(o.x,o.y)else lineTo(o.x,o.y)}}
            val fill=Path().apply{addPath(line);lineTo(coordinates.last().x,size.height);lineTo(coordinates.first().x,size.height);close()}
            drawPath(fill,Brush.verticalGradient(listOf(color.copy(alpha=.22f),color.copy(alpha=0f))))
            drawPath(line,color,style=Stroke(2.5.dp.toPx(),cap=StrokeCap.Round,join=StrokeJoin.Round))
            coordinates.forEach{drawCircle(Ink,4.dp.toPx(),it);drawCircle(color,2.7.dp.toPx(),it)}
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(points.first().date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM",Locale.forLanguageTag("es"))),fontSize=10.sp,color=Soft);Text(points.last().date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM",Locale.forLanguageTag("es"))),fontSize=10.sp,color=Soft)}
    }
}

@Composable fun WeekBars(values:List<Pair<String,Double?>>,unit:String,color:Color=Lime) {
    val max=maxOf(1.0,values.mapNotNull{it.second}.maxOrNull()?:1.0)
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(142.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.Bottom) {
            values.forEach{(day,value)->Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Text(if(value==null)"—" else number(value,if(unit=="h")1 else 0),fontSize=9.sp,color=if(value==null)Soft else White,maxLines=1)
                Box(Modifier.fillMaxWidth().height(96.dp).background(color.copy(alpha=.035f),RoundedCornerShape(6.dp)).semantics{contentDescription="$day: ${value?.let{number(it,1)+" "+unit}?:"sin registros"}"},contentAlignment=Alignment.BottomCenter) {
                    if(value!=null)Box(Modifier.fillMaxWidth().fillMaxHeight(maxOf(.025f,(value/max).toFloat())).background(Brush.verticalGradient(listOf(color,color.copy(alpha=.5f))),RoundedCornerShape(6.dp)))
                }
                Text(day,fontSize=10.sp,color=Soft)
            }}
        }
        Text("— Sin registros. Los días vacíos no se consideran cero.",fontSize=10.sp,color=Soft,lineHeight=16.sp)
    }
}
