package com.example.statstracker.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.example.statstracker.database.entity.GameStats
import com.example.statstracker.database.entity.Player
import com.example.statstracker.database.entity.PlayerGameStats
import com.example.statstracker.database.relation.GameWithTeamsAndEvents
import java.time.format.DateTimeFormatter

/**
 * Generates a PDF report for a game and saves it to the Downloads folder.
 * Returns the display name of the saved file on success.
 */
fun exportGamePdf(
    context: Context,
    gameWithDetails: GameWithTeamsAndEvents,
    teamStats: List<GameStats>,
    playerStats: List<PlayerGameStats>,
    homePlayers: List<Player>,
    awayPlayers: List<Player>
): String {
    val pageWidth = 842   // A4 landscape in points
    val pageHeight = 595
    val margin = 30f
    val contentWidth = pageWidth - 2 * margin

    val document = PdfDocument()
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas
    var y = margin

    // Paints
    val titlePaint = Paint().apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val subtitlePaint = Paint().apply {
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val bodyPaint = Paint().apply {
        textSize = 11f
        isAntiAlias = true
    }
    val headerPaint = Paint().apply {
        textSize = 7f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val cellPaint = Paint().apply {
        textSize = 7f
        isAntiAlias = true
    }
    val linePaint = Paint().apply {
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    // Helper: start a new page if needed, returns the new y position
    fun ensureSpace(needed: Float): Float {
        if (y + needed > pageHeight - margin) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            return margin
        }
        return y
    }

    // ─── Page Header: Game Info ────────────────────────────────────────
    val game = gameWithDetails.game
    val homeTeamName = gameWithDetails.homeTeam.name
    val awayTeamName = gameWithDetails.awayTeam.name

    canvas.drawText("$homeTeamName  vs  $awayTeamName", margin, y + 20f, titlePaint)
    y += 32f

    val dateStr = game.date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
    canvas.drawText("Date: $dateStr", margin, y + 11f, bodyPaint)
    y += 18f

    game.place?.let {
        canvas.drawText("Location: $it", margin, y + 11f, bodyPaint)
        y += 18f
    }

    game.notes?.takeIf { it.isNotBlank() }?.let {
        canvas.drawText("Notes: $it", margin, y + 11f, bodyPaint)
        y += 18f
    }

    y += 10f
    canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
    y += 16f

    // ─── Helper: Draw team section (title + player table + total row) ──
    fun drawTeamSection(teamName: String, players: List<Player>, teamId: Long) {
        val teamPlayerStats = playerStats.filter { it.teamId == teamId && it.quarter == null }

        // Team name title
        y = ensureSpace(40f)
        canvas.drawText(teamName, margin, y + 20f, titlePaint)
        y += 34f

        if (players.isEmpty() || teamPlayerStats.isEmpty()) {
            canvas.drawText("No player stats available", margin, y + 11f, bodyPaint)
            y += 20f
            return
        }

        // Table columns: Player | MIN | PTS | FGM/FGA | FG% | 3PM/3PA | 3P% | 2PM/2PA | 2P% | FTM/FTA | FT% | OREB | DREB | REB | AST | TOV | STL | BLK | PF | PIR | EFF | +/-
        // Relative weights; scaled below so they fill contentWidth exactly.
        val rawColumns = listOf(
            "Player" to 80f,
            "MIN" to 30f,
            "PTS" to 24f,
            "FGM/FGA" to 38f,
            "FG%" to 28f,
            "3PM/3PA" to 38f,
            "3P%" to 28f,
            "2PM/2PA" to 38f,
            "2P%" to 28f,
            "FTM/FTA" to 38f,
            "FT%" to 28f,
            "OREB" to 28f,
            "DREB" to 28f,
            "REB" to 24f,
            "AST" to 24f,
            "TOV" to 24f,
            "STL" to 24f,
            "BLK" to 24f,
            "PF" to 22f,
            "PIR" to 24f,
            "EFF" to 24f,
            "+/-" to 26f
        )
        val rawTotal = rawColumns.sumOf { it.second.toDouble() }.toFloat()
        val scale = contentWidth / rawTotal
        val columns = rawColumns.map { (name, w) -> name to w * scale }

        // Draw header row
        y = ensureSpace(18f)
        var x = margin
        for ((colName, colWidth) in columns) {
            canvas.drawText(colName, x, y + 10f, headerPaint)
            x += colWidth
        }
        y += 14f
        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
        y += 4f

        // Accumulators for totals
        var totPts = 0; var totFgm = 0; var totFga = 0
        var totTpm = 0; var totTpa = 0; var totFtm = 0; var totFta = 0
        var totOreb = 0; var totDreb = 0; var totReb = 0; var totAst = 0; var totStl = 0
        var totBlk = 0; var totTo = 0; var totPf = 0
        var totPir = 0; var totEff = 0; var totPlusMinus = 0

        // Draw player rows
        for (player in players) {
            val ps = teamPlayerStats.find { it.playerId == player.id } ?: continue
            y = ensureSpace(16f)
            x = margin

            totPts += ps.points
            totFgm += ps.fieldGoalsMade; totFga += ps.fieldGoalsAttempted
            totTpm += ps.threePointersMade; totTpa += ps.threePointersAttempted
            totFtm += ps.freeThrowsMade; totFta += ps.freeThrowsAttempted
            totOreb += ps.reboundsOffensive; totDreb += ps.reboundsDefensive
            totReb += ps.totalRebounds; totAst += ps.assists
            totStl += ps.steals; totBlk += ps.blocks
            totTo += ps.turnovers; totPf += ps.foulsPersonal
            totPir += ps.pir; totEff += ps.efficiency
            totPlusMinus += ps.plusMinus

            val name = "${player.firstName.first()}. ${player.lastName}"
            val truncatedName = if (name.length > 12) name.take(11) + "\u2026" else name

            val fgPct = if (ps.fieldGoalsAttempted > 0) String.format("%.1f", ps.fieldGoalPercentage * 100) else "-"
            val tpPct = if (ps.threePointersAttempted > 0) String.format("%.1f", ps.threePointPercentage * 100) else "-"
            val twoPct = if (ps.twoPointersAttempted > 0) String.format("%.1f", ps.twoPointPercentage * 100) else "-"
            val ftPct = if (ps.freeThrowsAttempted > 0) String.format("%.1f", ps.freeThrowPercentage * 100) else "-"
            val pmStr = if (ps.plusMinus >= 0) "+${ps.plusMinus}" else "${ps.plusMinus}"

            val values = listOf(
                truncatedName,
                formatMinutes(ps.timePlayedSeconds),
                "${ps.points}",
                "${ps.fieldGoalsMade}/${ps.fieldGoalsAttempted}",
                fgPct,
                "${ps.threePointersMade}/${ps.threePointersAttempted}",
                tpPct,
                "${ps.twoPointersMade}/${ps.twoPointersAttempted}",
                twoPct,
                "${ps.freeThrowsMade}/${ps.freeThrowsAttempted}",
                ftPct,
                "${ps.reboundsOffensive}",
                "${ps.reboundsDefensive}",
                "${ps.totalRebounds}",
                "${ps.assists}",
                "${ps.turnovers}",
                "${ps.steals}",
                "${ps.blocks}",
                "${ps.foulsPersonal}",
                "${ps.pir}",
                "${ps.efficiency}",
                pmStr
            )

            for (i in columns.indices) {
                canvas.drawText(values[i], x, y + 10f, cellPaint)
                x += columns[i].second
            }
            y += 16f
        }

        // Total row
        y = ensureSpace(20f)
        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint)
        y += 4f
        x = margin

        val totFgPct = if (totFga > 0) String.format("%.1f", totFgm.toDouble() / totFga * 100) else "-"
        val totTpPct = if (totTpa > 0) String.format("%.1f", totTpm.toDouble() / totTpa * 100) else "-"
        val tot2pm = totFgm - totTpm
        val tot2pa = totFga - totTpa
        val totTwoPct = if (tot2pa > 0) String.format("%.1f", tot2pm.toDouble() / tot2pa * 100) else "-"
        val totFtPct = if (totFta > 0) String.format("%.1f", totFtm.toDouble() / totFta * 100) else "-"
        val totPmStr = if (totPlusMinus >= 0) "+$totPlusMinus" else "$totPlusMinus"

        val totalValues = listOf(
            "TOTAL",
            "",  // no total for minutes
            "$totPts",
            "$totFgm/$totFga",
            totFgPct,
            "$totTpm/$totTpa",
            totTpPct,
            "$tot2pm/$tot2pa",
            totTwoPct,
            "$totFtm/$totFta",
            totFtPct,
            "$totOreb",
            "$totDreb",
            "$totReb",
            "$totAst",
            "$totTo",
            "$totStl",
            "$totBlk",
            "$totPf",
            "$totPir",
            "$totEff",
            totPmStr
        )

        for (i in columns.indices) {
            canvas.drawText(totalValues[i], x, y + 10f, headerPaint)
            x += columns[i].second
        }
        y += 20f
    }

    // ─── Draw Home Team ────────────────────────────────────────────────
    drawTeamSection(homeTeamName, homePlayers, gameWithDetails.homeTeam.id)

    // ─── Force new page for Away Team ──────────────────────────────────
    document.finishPage(page)
    pageNumber++
    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    page = document.startPage(pageInfo)
    canvas = page.canvas
    y = margin

    // ─── Draw Away Team ────────────────────────────────────────────────
    drawTeamSection(awayTeamName, awayPlayers, gameWithDetails.awayTeam.id)

    // Finish last page
    document.finishPage(page)

    // ─── Save to Downloads via MediaStore ──────────────────────────────
    val safeHome = homeTeamName.replace(Regex("[^a-zA-Z0-9]"), "_")
    val safeAway = awayTeamName.replace(Regex("[^a-zA-Z0-9]"), "_")
    val dateFile = game.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val fileName = "Game_${safeHome}_vs_${safeAway}_$dateFile.pdf"

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw IllegalStateException("Failed to create MediaStore entry")

    resolver.openOutputStream(uri).use { outputStream ->
        if (outputStream == null) throw IllegalStateException("Failed to open output stream")
        document.writeTo(outputStream)
    }
    document.close()

    return fileName
}

private fun formatPct(value: Double): String {
    return "${(value * 100).toInt()}%"
}

private fun formatMinutes(seconds: Int): String {
    if (seconds <= 0) return "0:00"
    val min = seconds / 60
    val sec = seconds % 60
    return "$min:${String.format("%02d", sec)}"
}
