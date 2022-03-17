package seamcarving

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Math.pow
import javax.imageio.ImageIO
import kotlin.math.*

lateinit var im: BufferedImage
lateinit var imEnergyX: Array<Array<Array<Double>>>
lateinit var imEnergyY: Array<Array<Array<Double>>>

fun parseArgs(args: Array<String>): List<String> {
    fun findOption(optionName: String): String {
        val indexOption = args.indexOf(optionName)
        return if (indexOption >= 0 && indexOption != (args.size - 1)) args[indexOption + 1] else "???"
    }
    val inputFile = findOption("-in")
    val outputFile = findOption("-out")
    return listOf(inputFile, outputFile)
}

fun color(j: Int, x: Int, y: Int): Int {
    val color = Color(im.getRGB(x, y))
    when(j) {
        1 -> return color.red
        2 -> return color.green
        3 -> return color.blue
    }
    return -1
}

fun colorD(i: Int, j: Int, x: Int, y: Int): Int = color(j, x + (2 - i), y + (i - 1)) - color(j, x - (2 - i), y - (i - 1))

fun delta(i:Int, x: Int, y: Int): Double {
    var sum = 0.0
    for (j in 1..3) {
        sum += colorD(i, j, x, y) * colorD(i, j, x, y)
    }
    return sum
}

fun energy(x: Int, y: Int): Double {
    val w = im.width - 1
    val h = im.height - 1
    val result = when {
        (x == 0 && y == 0) -> delta(1, 1, 0) + delta(2, 0, 1)
        (x == w && y == h) -> delta(1, w - 1, h) + delta(2, w, h - 1)
        (x == 0 && y == h) -> delta(1, 1, h) + delta(2, 0, h - 1)
        (x == w && y == 0) -> delta(1, w - 1, 0) + delta(2, w, 1)
        (x == 0) -> delta(1, 1, y) + delta(2, 0, y)
        (y == 0) -> delta(1, x, 0) + delta(2, x, 1)
        (x == w) -> delta(1, x - 1, y) + delta(2, x, y)
        (y == h) -> delta(1, x, y) + delta(2, x, y - 1)
        else -> delta(1, x, y) + delta(2, x, y)
    }
    return sqrt(result)
}

fun initArrayEnergy() {
    imEnergyX = Array(im.width) { Array(im.height) { Array(2) { 0.0 } } }
    imEnergyY = Array(im.width) { Array(im.height) { Array(2) { 0.0 } } }

    for (x in 0 until im.width) {
        for (y in 0 until im.height) {
            val en = energy(x, y)
            imEnergyX[x][y][0] = en
            if (y == 0) imEnergyX[x][y][1] = en
        }
    }

    for (y in 0 until im.height) {
        for (x in 0 until im.width) {
            val en = energy(x, y)
            imEnergyY[x][y][0] = en
            if (x == 0) imEnergyY[x][y][1] = en
        }
    }
}

fun minEnergyAboveX(x: Int, y: Int): Int {
    var minE = Double.MAX_VALUE
    var minX = 0
    for ( col in x - 1 .. x + 1) {
        if (0 <= col && col < im.width) {
            val en = imEnergyX[col][y - 1][1]
            if ( en < minE) {
                minX = col
                minE = en
            }
        }
    }
    return minX
}

fun minEnergyAboveY(x: Int, y: Int): Int {
    var minE = Double.MAX_VALUE
    var minY = 0
    for ( row in y - 1 .. y + 1) {
        if (0 <= row && row < im.height) {
            val en = imEnergyX[x - 1][row][1]
            if ( en < minE) {
                minY = col
                minE = en
            }
        }
    }
    return minY
}
fun fillArrayEnergy() {
    for (y in 1 until im.height) {
        for (x in 0 until im.width) {
            imEnergyX[x][y][1] = imEnergyX[x][y][0] + imEnergyX[minEnergyAboveX(x, y)][y - 1][1]
        }
    }
    for (x in 0 until im.width) {
        for (y in 1 until im.height) {
            imEnergyX[x][y][1] = imEnergyX[x][y][0] + imEnergyX[x - 1][minEnergyAboveY(x, y)][1]
        }
    }
}

fun minSumEnergyX(): Int {
    var min = Double.MAX_VALUE
    var minX = -1
    for (x in 0 until im.width) {
        val en = imEnergyX[x][im.height - 1][1]
        if ( en < min) {
            min = en
            minX = x
        }
    }
    return minX
}

fun main(args: Array<String>) {
    val (inputFileName, outputFileName) = parseArgs(args)
    val inputFile = File(inputFileName)
    val outputFile = File(outputFileName)
    im  = ImageIO.read(inputFile)
    val imOut = BufferedImage(im.width, im.height, BufferedImage.TYPE_INT_RGB)

    initArrayEnergy()
    fillArrayEnergy()

    val colorNew = Color(255, 0, 0)
    var minX = minSumEnergyX()

    for (y in im.height - 1 downTo 1) {
        im.setRGB(minX, y, colorNew.rgb)
        minX = minEnergyAboveX(minX, y)
    }
    im.setRGB(minX, 0, colorNew.rgb)

    ImageIO.write(im, "png", outputFile)
}