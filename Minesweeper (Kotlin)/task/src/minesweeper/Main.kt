package minesweeper

import kotlin.random.Random

const val NUM_OF_MINES_MESSAGE = "How many mines do you want on the field? "
const val TURN_MESSAGE = "Set/unset mines marks or claim a cell as free: "
const val LOSE_MESSAGE = "You stepped on a mine and failed!"
const val WIN_MESSAGE = "Congratulations! You found all the mines!"

fun main() {

    print(NUM_OF_MINES_MESSAGE)
    val numOfMines = readln().toInt()
    val mineSweeper = MineSweeper(numOfMines)
    mineSweeper.printMinefield()

    do {
        print(TURN_MESSAGE)
        try {
            val (col, row, moveType) = readln().split(" ")
            mineSweeper.move(row, col, moveType)
            mineSweeper.printMinefield()
        } catch (e: IndexOutOfBoundsException) {
            println("Invalid input.")
        }
    } while (!mineSweeper.isGameEnds)

    if (mineSweeper.isLose) {
        println(LOSE_MESSAGE)
    } else {
        println(WIN_MESSAGE)
    }
}

class MineSweeper(private val numOfMines: Int = 0, private val size: Int = SIZE) {

    private var isFirstOpen = true

    private val boardCells = mutableSetOf<Pair<Int, Int>>()
    private val mineCells = mutableSetOf<Pair<Int, Int>>()
    private val numberedCells = mutableMapOf<Pair<Int, Int>, String>()
    private val markedCells = mutableSetOf<Pair<Int, Int>>()
    private val blankCells = mutableSetOf<Pair<Int, Int>>()
    private val openedCells = mutableSetOf<Pair<Int, Int>>()
    private val minefield = mutableListOf<MutableList<String>>()

    private val directions = listOf(
        -1 to -1, // up left
        -1 to 0,  // up
        -1 to 1,  // up right
        0 to 1,   // right
        1 to 1,   // down right
        1 to 0,   // down
        1 to -1,  // down left
        0 to -1   // left
    )

    var isLose = false
    val isWin: Boolean
        get() {
            return hasUserGuessedAllCorrectly() || isAllBlankCellsOpened()
        }

    val isGameEnds: Boolean
        get() {
            return !isMineCellsEmpty() && (isLose || isWin)
        }

    init {
        generateInitialBoard()
    }


    private fun isMineCellsEmpty(): Boolean = mineCells.isEmpty()

    private fun hasUserGuessedAllCorrectly(): Boolean = mineCells == markedCells

    private fun generateMinefield(excludedCell: Pair<Int, Int>) {
        val mineCoordinates = generateRandomCoordinates(numOfMines, excludedCell)
        mineCells.addAll(mineCoordinates)
    }

    private fun generateInitialBoard() {
        for (row in 0 until size) {
            minefield.add(mutableListOf())
            for (col in 0 until size) {
                minefield[row].add(UNOPENED)
                boardCells.add(Pair(row, col))
            }
        }
    }

    private fun generateRandomCoordinates(
        numOfCoordinates: Int,
        excludedCell: Pair<Int, Int>
    ): Set<Pair<Int, Int>> {
        val coordinates = mutableSetOf<Pair<Int, Int>>()
        repeat(numOfCoordinates) {
            var coordinate: Pair<Int, Int>
            do {
                coordinate = Pair(Random.nextInt(0, size), Random.nextInt(0, size))
                val isValidPair = !coordinates.contains(coordinate) && coordinate != excludedCell
            } while (!isValidPair)
            coordinates.add(coordinate)
        }
        return coordinates
    }

    private fun generateNumbers() {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (!isMine(row, col)) {
                    val mines = directions.count { (rowOffset, colOffset) ->
                        val nextRow = row + rowOffset
                        val nextCol = col + colOffset

                        isValidCoordinate(nextRow, nextCol) && isMine(nextRow, nextCol)
                    }

                    if (mines > 0) numberedCells[Pair(row, col)] = mines.toString()
                }
            }
        }
    }

    private fun generateBlanks() {
        blankCells.addAll(boardCells)
        blankCells.removeAll(mineCells)
        blankCells.removeAll(numberedCells.keys)
    }

    fun printMinefield() {

        println(" |123456789|")
        println("-|---------|")

        for (row in minefield.indices) {

            print("${row + 1}|")
            for (col in minefield[row].indices) {
                print(minefield[row][col])
            }
            println("|")
        }

        println("-|---------|")
    }

    private fun isValidCoordinate(row: Int, col: Int): Boolean {
        return row in 0..<size && col in 0..<size
    }

    private fun isNotOpened(row: Int, col: Int): Boolean {
        return !openedCells.contains(Pair(row, col))
    }

    private fun isMine(row: Int, col: Int): Boolean {
        return mineCells.contains(Pair(row, col))
    }

    private fun isBlank(row: Int, col: Int): Boolean {
        return blankCells.contains(Pair(row, col))
    }

    private fun String.isMine(): Boolean {
        return this == MINE
    }

    fun move(rowString: String, colString: String, moveType: String) {
        try {
            val row = rowString.toInt() - 1
            val col = colString.toInt() - 1

            when(moveType) {
                MARK -> markCell(row, col)
                OPEN -> openCell(row, col)
            }

        } catch(e: Exception) {
            println("Invalid Input")
        }
    }

    private fun markCell(row: Int, col: Int) {
        if (markedCells.contains(Pair(row, col))) {
            markedCells.remove(Pair(row, col))
            minefield[row][col] = UNOPENED
        } else {
            markedCells.add(Pair(row, col))
            minefield[row][col] = MARKED
        }
    }

    private fun openCell(row: Int, col: Int) {
        if (isFirstOpen) {
            isFirstOpen = false
            generateBoard(excludedCell = Pair(row, col))
        }

        if (isMine(row, col)) {
            isLose = true
            fillAllMine()
        } else {
            fillCell(row, col)
        }
    }

    private fun fillCell(row: Int, col: Int) {
        if (isBlank(row, col)) {
            openedCells.add(Pair(row, col))
            minefield[row][col] = BLANK

            directions.forEach { (rowOffset, colOffset) ->
                val nextRow =  row + rowOffset
                val nextCol = col + colOffset

                if (isValidCoordinate(nextRow, nextCol) && isNotOpened(nextRow, nextCol)) {
                    fillCell(nextRow, nextCol)
                }
            }
        }

        if (isNumber(row, col)) {
            openedCells.add(Pair(row, col))
            minefield[row][col] = numberedCells[Pair(row, col)].toString()
        }
    }

    private fun isNumber(row: Int, col: Int): Boolean {
        return numberedCells.keys.contains(Pair(row, col))
    }

    private fun clearMarkedCells() {
        markedCells.clear()
    }

    private fun clearMinefield() {
        minefield.clear()
    }

    private fun generateBoard(excludedCell: Pair<Int, Int>) {
//        clearMarkedCells()
//        clearMinefield()
//        generateInitialBoard()
        generateMinefield(excludedCell)
        generateNumbers()
        generateBlanks()
    }

    private fun isAllBlankCellsOpened(): Boolean {
       return openedCells.containsAll(blankCells)
    }

    private fun fillAllMine() {
        for((row, col) in mineCells) {
            minefield[row][col] = MINE
        }
    }

    companion object {
        const val UNOPENED = "."
        const val MINE = "X"
        const val MARKED = "*"
        const val BLANK = "/"
        const val SIZE = 9

        const val MARK = "mine"
        const val OPEN = "free"
    }
}