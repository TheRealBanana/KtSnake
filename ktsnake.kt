import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import kotlin.math.floor


data class ColorTuple(val r: Float, val g: Float, val b: Float)
data class Grid(val row: Int, val col: Int)
data class Point(val x: Int, val y: Int)


enum class GridElementTypes(val color: ColorTuple){
    DEFAULT_COLOR(ColorTuple(0.0f,0.0f,0.0f)),
    SNAKE_HEAD(ColorTuple(0.25882f,1.0f,0.0f)),
    SNAKE_TAIL(ColorTuple(0.15686f,0.58823f,0.0f)),
    OBJECTIVE(ColorTuple(0.0f,0.32941f,0.65098f)),
    DEAD_HEAD(ColorTuple(1.0f,0.0f,0.0f))
}

class GameGrid(rows: Int, cols: Int, side_size_px: Int) {
    private val rows: Int
    private val cols: Int
    private val sidesizepx: Int
    private val activeGridElements: MutableMap<Grid, GridElement>

    init {
        this.rows = rows
        this.cols = cols
        this.sidesizepx = side_size_px
        activeGridElements = mutableMapOf<Grid, GridElement>()
    }

    fun getGridElement(grid_index: Grid): GridElement? {
        when (activeGridElements.containsKey(grid_index)) {
            true -> return activeGridElements.getValue(grid_index)
            false -> return null
        }

    }
    fun createGridElement(element_type: GridElementTypes, grid_index: Grid): Boolean{
        if (activeGridElements.containsKey(grid_index)) {
            return false
        } else {
            val xcoord: Int = sidesizepx * grid_index.row + sidesizepx
            val ycoord: Int = sidesizepx * grid_index.col + sidesizepx
            val origin_coords = Point(xcoord, ycoord)
            val new_grid_element: GridElement = GridElement(element_type.color, origin_coords, sidesizepx)
            activeGridElements[grid_index] =  new_grid_element
            return true
        }
    }
    fun deleteGridElement(grid_index: Grid){
        if (activeGridElements.containsKey(grid_index)) {
            activeGridElements.remove(grid_index)
        } else {
            throw Exception("Tried to delete non-existent grid at index %s".format(grid_index.toString()))
        }
    }

    fun redrawGrid() {
        glClear(GL_COLOR_BUFFER_BIT)
        for ((_, grid_element) in activeGridElements) {
            grid_element.draw()
        }
    }

    fun clearGrid() {
        activeGridElements.clear()
    }

}

class GridElement (type: ColorTuple, origin_coords: Point, size_px: Int) {
    private val color: ColorTuple
    private val origincoords: Point
    private val sizepx: Int

    init {
        this.color = type
        this.origincoords = origin_coords
        this.sizepx = size_px
    }

    fun draw() {
        // Figure out our vertices
        val br: Point = origincoords
        val tr: Point = Point(origincoords.x, origincoords.y - sizepx)
        val tl: Point = Point(origincoords.x - sizepx, origincoords.y - sizepx)
        val bl: Point = Point(origincoords.x - sizepx, origincoords.y)
        glColor3f(color.r, color.g, color.b)
        glBegin(GL_QUADS)
        glVertex2i(br.x, br.y)
        glVertex2i(tr.x, tr.y)
        glVertex2i(tl.x, tl.y)
        glVertex2i(bl.x, bl.y)
        glEnd()
    }
}


object SnakeGame {
    private const val WINDOW_SIZE_WIDTH: Int = 500
    private const val WINDOW_SIZE_HEIGHT: Int = 500
    private const val side_size_px: Int = 25
    private val rows: Int = floor(WINDOW_SIZE_WIDTH.toFloat()/side_size_px.toFloat()).toInt()
    private val cols = floor(WINDOW_SIZE_HEIGHT.toFloat()/side_size_px.toFloat()).toInt()
    private val gameGrid: GameGrid = GameGrid(rows, cols, side_size_px)
    private var window: Long = NULL

    fun startGame() {
        //initialize GLFW
        init(WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)

        //Create a single grid element in the upper-left corner (0,0)
        gameGrid.createGridElement(GridElementTypes.DEAD_HEAD, Grid(0,0))

        //Draw it!
        while (!glfwWindowShouldClose(window)) {
            gameGrid.redrawGrid()
            glfwSwapBuffers(window)
            glfwPollEvents()
            //Thread.sleep(1000L)
        }
    }

    private fun init(windowSizeW: Int, windowSizeH: Int) {
        if ( !glfwInit() ) {
            throw Exception("Failed to initialize GLFW.")
        }
        glfwDefaultWindowHints()
        //Do not allow resize
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        window = glfwCreateWindow(windowSizeW, windowSizeH, "KtSnake", 0, 0)
        if (window == NULL) {
            throw Exception("Failed to initialize window.")
        }
        glfwMakeContextCurrent(window)
        // GL configuration comes AFTER we make the window our current context, otherwise errors
        GL.createCapabilities()
        glClearColor(0.0f,0.0f,0.0f,1.0f)
        glOrtho(0.0, WINDOW_SIZE_WIDTH.toDouble(), WINDOW_SIZE_HEIGHT.toDouble(), 0.0, -1.0, 1.0)
        glViewport(0, 0, WINDOW_SIZE_WIDTH, WINDOW_SIZE_HEIGHT)
        glfwShowWindow(window)
    }
}

fun main(args: Array<String>) {
    println("here we go again lol")
    SnakeGame.startGame()
}