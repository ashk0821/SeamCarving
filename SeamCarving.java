import java.util.ArrayList;
import java.util.Objects;
import java.util.Iterator;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.*;
import java.awt.Color;

// Contains method for ArrayList<SeamInfo>
class Utils {
  // finds the SeamInfo in the given array list with the minimum total weight
  // if the ArrayList is ordered left -> right, priority is given to leftmost
  // pixel
  // if the ArrayList is ordered top -> bottom, priority is given to topmost
  // pixel
  SeamInfo minTotalWeight(ArrayList<SeamInfo> possibleCameFroms) {
    if (possibleCameFroms.size() == 0) {
      throw new IllegalStateException("List of possible seams is empty.");
    }

    // sets the minimum seam to the first seam in the list
    SeamInfo min = possibleCameFroms.get(0);

    // iterates through all the rest of the seamInfos in the list and compares it to
    // the seam with the minimum total weight do far, updating the minimum if necessary.
    for (int i = 1; i < possibleCameFroms.size(); i += 1) {
      SeamInfo curSeam = possibleCameFroms.get(i);
      
      // if the current seam accessed is more boring than the stored minimum seam,
      // set the minimum seam to the current seam
      if (curSeam.moreBoringSeam(min)) {
        min = curSeam;
      }
    }
    return min;
  }
}

// represents the world state of the big bang
class BigBangWorld extends World {
  
  ImageWorld imgWorld; // represents the current state of the seam carver,
  // which contains all the pixels
  
  // true if vertical seams are being deleted,
  // false if horizontal seams are being deleted
  boolean seamsVertical; 
  
  // true if the previous tick was a deletion of a seam (or if there was no first tick), 
  // false if the previous tick was coloring the most boring seam red
  boolean deletedLastTick; 
  
  // true if the seams will be removed automatically, false if the seams are to be removed
  // manually 
  boolean automatic;
  
  // true if the energy of each pixel should be displayed, false if otherwise
  boolean energyGrayscale;
 
  // true if the cumulative weight of each seam should be displayed, false if otherwise
  boolean seamGrayscale;
  
  // true if we are adding deleted seam back into the picture
  boolean addingSeams;
  
  // intializes the current seam carver _imgWorld_ 
  // and whether or not the seams are vertical _seamsVertical_.
  // sets deletedLastTick to true, as the next tick should be painting a seam red
  // sets automatic to true, as the world has not been paused yet
  // sets energyGrayscale and seamGrayscale to false 
  // so the actual colors of the pixels can be seen by default
  // addingSeams is set to false to default, since we begin by removing them
  BigBangWorld(ImageWorld imgWorld, boolean seamsVertical) {
    this.imgWorld = imgWorld;
    this.seamsVertical = seamsVertical;
    
    this.deletedLastTick = true;
    this.automatic = true;
    this.energyGrayscale = false;
    this.seamGrayscale = false;
    this.addingSeams = false;
  }
  
  // method that is called each tick. 
  // if adding seams is on, then we add back in the most recently deleted seam
  // if adding seams is off, then we go back and forth between painting the
  // most borign seam red and deleting that seam
  // If automatic is off, then the world is paused, so the ticks do nothing.
  public void onTick() {
    
    if (this.imgWorld.shouldFlipAddingSeams()) {
      this.addingSeams = !addingSeams;
    }
    
    // the world should only progress if automatic is on
    if (this.automatic) {
      
      // if we are not currently adding seams, we are deleting them instead. 
      // Every other time, we are either
      // highlighting the most boring seam in red, or deleting that seam.
      if (!this.addingSeams) {
      
     // if we deleted a seam last tick, 
        // then we should find a new seam to delete and paint it red this tick
      if (this.deletedLastTick) {
        
        // before we find the new boring seam, we should regenerate our seams
        if (this.seamsVertical) {
          imgWorld.generateVerticalSeams();
        }
        else {
          imgWorld.generateHorizontalSeams();
        }
        
        // find the next boring seam and paint it red
        imgWorld.setBoringSeam(this.seamsVertical);
        imgWorld.paintBoringSeamRed();
      }
      
      // if we did not delete a seam last tick, then we delete the seam we set aside.
      else {
        imgWorld.deleteBoringSeam(this.seamsVertical);
      }
      // reverse deletedLastTick so the next onTick call will do the opposite job
      this.deletedLastTick = !this.deletedLastTick;
      
      // if we are currently adding seams,
      // then we add back in the seam most recently deleted
    } else {
      this.imgWorld.addRecentlyDeletedSeam(seamsVertical);
    } 
    }
  }
  
  // method that is called during key clicks
  public void onKeyEvent(String key) {
    // if the user presses the space key, pause/unpause 
    // the automatic removal of seams
    if (key.equals(" ")) {
      // We don't want to pause in the middle of painting a seam red,
      // so if the next tick is meant to delete a red seam, then tick
      // once to delete that seam before pausing
      if (!this.deletedLastTick) {
        this.onTick();
      }
      this.automatic = !this.automatic;
    }
    
    // if the user presses the "e" key, convert/revert
    // the image to the grayscale of the energy of pixels
    if (key.equals("e")) {
      this.energyGrayscale = !this.energyGrayscale;
      // we can't show energy grayscale ans seam grayscale at the same time,
      // so we need to make sure seam grayscale is set to false
      this.seamGrayscale = false;
    }
    
    // if the user presses the "S" key, convert/revert
    // the image to the grayscale of the cumulative weight of each seam
    if (key.equals("s")) {
      this.seamGrayscale = !this.seamGrayscale;
      // we can't show energy grayscale ans seam grayscale at the same time,
      // so we need to make sure energy grayscale is set to false
      this.energyGrayscale = false;
    }
    
    // if the automatic removal of seams is paused,
    // delete a vertical seam if the "v" key is pressed or
    // delete a horizontal seam if the "h" key is pressed
    if (!this.automatic) {
      if (key.equals("v")) {
        imgWorld.generateVerticalSeams();
        imgWorld.setBoringSeam(true);
        imgWorld.deleteBoringSeam(true);
      }
      else if (key.equals("h")) {
        imgWorld.generateHorizontalSeams();
        imgWorld.setBoringSeam(false);
        imgWorld.deleteBoringSeam(false);
      }
    }
  }
  
  public boolean shouldWorldEnd() {
    return this.addingSeams && this.imgWorld.deletedSeams.isEmpty();
  }

  // method that renders the world as an image
  public WorldScene makeScene() {
    ComputedPixelImage cpi = imgWorld.createCPI(energyGrayscale, seamGrayscale, seamsVertical);
    WorldScene scene = new WorldScene(500, 500);
    scene.placeImageXY(cpi, 250, 250);
    return scene;
  }
  
}

// represents the world state of the seam carver
class ImageWorld  implements Iterable<Iterable<APixel>>{
  FromFileImage ogImg; // the original photo
  Sentinel topleft; // the sentinel node in the top left of the grid
  Sentinel topright; // the sentinel node in the top right of the grid
  Sentinel bottomleft; // the sentinel node in the bottom left of the grid
  Sentinel bottomright; // the sentinel node in the bottom right of the grid
  

  // the current width of the photo, which will decrease every time 
  // a vertical seam is removed.
  int width;

  // the current height of the photo, which will decrease every time 
  // a horizontal seam is removed
  int height; 

  // the current most boring seam that will be deleted on the next tick that
  // a seam has to be deleted on 
  SeamInfo seamToDelete; 
  
  // List of deleted seams in the order that they were deleted
  // with the last one deleted being the first in the stack
  Stack<SeamInfo> deletedSeams;

  // Initializes the original image to the given image,
  // sets the width and height to the dimensions of that image,
  // creates the grid of pixels based on the original image,
  // and generates seams based on whether the seams will be vertical or horizontal
  // sets deletedSeams to an empty stack
  ImageWorld(FromFileImage ogImg, boolean seamsVertical) {
    this.ogImg = ogImg;
    this.width = (int) ogImg.getWidth();
    this.height = (int) ogImg.getHeight();
    
    this.populate();
    
    if(seamsVertical) {
      this.generateVerticalSeams();
    } else {
      this.generateHorizontalSeams();
    } 
    
    this.deletedSeams = new Stack<SeamInfo>(new ArrayList<SeamInfo>());
  }
  
  // ends the world is the width or height of the world has a single line of pixels left
  public boolean shouldFlipAddingSeams() {
    return this.finishedDeletingSeams();
  }

  // Sets the next seam to delete to the most boring seam in the seam carver,
  // either horizontal or vertical depending on the parameter.
  void setBoringSeam(boolean vertical) {
    if(vertical) {
      this.seamToDelete = this.findMostBoringVertSeam();
    }
    else {
      this.seamToDelete = this.findMostBoringHorizSeam();
    }
    
  }
  
  // paints the currently stored most boring seam reds
  public void paintBoringSeamRed() {
    this.seamToDelete.paintSeamRed();
  }
  
  // deletes the currently stored most boring seam
  // takes in a boolean to distinguish whether to delete
  // a vertical or horizontal seam.
  // Regenerates the seams after the most boring seam is deleted 
  // so the next most boring seam can be found more accuratly with its new neighbors.
  public void deleteBoringSeam(boolean vertical) {
    this.deletedSeams.push(seamToDelete);
    if (vertical) {
      seamToDelete.deleteSeamVert(height - 1, height);
      this.width -= 1;
      this.generateVerticalSeams();
    }
    else {
      seamToDelete.deleteSeamHoriz(width - 1, width);
      this.height -= 1;
      this.generateHorizontalSeams();
    }
  }
  
  public void addRecentlyDeletedSeam(boolean vertical) {
    SeamInfo seamToAdd = this.deletedSeams.pop();
    if (vertical) {
      seamToAdd.addSeamVert(height - 1, height);
      this.width += 1;
      //this.generateVerticalSeams();
    }
    else {
      seamToAdd.addSeamHoriz(width - 1, width);
      this.height += 1;
     // this.generateHorizontalSeams();
    }
  }

  // fills this world with sentinels and pixels, with the correct neighbors
  // these pixels are now accessible through the topleft, topright, bottomleft,
  // and bottomright fields.
  void populate() {
    this.createSentinelFrame();
    this.findSentinelAt("top", 1).setDown(this.createPixelColumns(0));
  }
  
  // Decides if there are no more seams to delete
  // There are no more seams to delete 
  // if the width or height of the world has a single line of pixels left
  public boolean finishedDeletingSeams() {
    return this.width < 2 || this.height < 2;
  }

  // generates the sentinels needed to render this image
  void createSentinelFrame() {

    // ONLY createTopSentinels is width + 1 because we have to generate both the
    // topleft and topright sentinels.
    // all others we only need to generate one or zero corners

    this.topleft = this.createTopSentinels(width + 1);
    this.topleft.setLeft(this.topright);

    this.topright.setDown(this.createRightSentinels(height));
    this.bottomright.setDown(this.topright);

    this.bottomright.setLeft(this.createBottomSentinels(width));
    this.bottomright.setRight(this.bottomleft);
    this.bottomleft.setDown(this.topleft);

    this.bottomleft.setUp(this.createLeftSentinels(height));
  }

  // generate the sentinels on the top row,
  // and returns the topleft sentinel, 
  // which is connected to the rest of the top row 
  // through the line of left/right neighbors
  public Sentinel createTopSentinels(int i) {
    if (i == 0) {
      Sentinel tr = new Sentinel();
      this.topright = tr;
      return tr;
    }
    else {
      Sentinel newSen = new Sentinel();
      Sentinel rightRow = createTopSentinels(i - 1);
      newSen.setRight(rightRow);
      return newSen;
    }
  }

  // generates the sentinels on the right column
  // and returns the sentinel below the topright sentinel,
  // which is connected to the rest of the right column
  // through the line of up/down neighbors
  public Sentinel createRightSentinels(int i) {
    if (i == 0) {
      Sentinel br = new Sentinel();
      this.bottomright = br;
      return br;
    }
    else {
      Sentinel newSen = new Sentinel();
      Sentinel downColumn = createRightSentinels(i - 1);
      newSen.setDown(downColumn);
      return newSen;
    }
  }

  // generates the sentinels on the bottom row
  // and returns the sentinel to the left of the bottomright sentinel,
  // which is connected to the rest of the bottom row
  // through the line of left/right neighbors
  public Sentinel createBottomSentinels(int i) {
    if (i == 0) {
      Sentinel bl = new Sentinel();
      this.bottomleft = bl;
      return bl;
    }
    else {
      Sentinel newSen = new Sentinel();
      newSen.setDown(this.findSentinelAt("top", i));

      Sentinel leftRow = createBottomSentinels(i - 1);
      newSen.setLeft(leftRow);
      return newSen;
    }
  }

  // generates the sentinels on the left column
  // and returns the sentinel above the bottomleft sentinel,
  // which is connected to the rest of the left column
  // through the line of up/down neighbors
  public Sentinel createLeftSentinels(int i) {
    if (i == 0) {
      return this.topleft;
    }
    else {
      Sentinel newSen = new Sentinel();
      newSen.setLeft(this.findSentinelAt("right", i));
      Sentinel upRow = createLeftSentinels(i - 1);
      newSen.setUp(upRow);
      return newSen;
    }
  }

  // creates a new pixel using the color that the original image is
  // at the specified coordinates
  public Pixel createPixel(int x, int y) {
    Color pxColor = ogImg.getColorAt(x, y);
    int redAmt = pxColor.getRed();
    int greenAmt = pxColor.getGreen();
    int blueAmt = pxColor.getBlue();
    return new Pixel(redAmt, greenAmt, blueAmt);
  }

  // finds the pixel in the graph at the specified coordinates
  public APixel findPixelAt(int x, int y) {
    APixel origin = this.topleft.getDownRight();
    APixel curPixel = origin;
    // scans right until the pixel is in line with the x-coordinate
    for (int i = 0; i < x; i += 1) {
      curPixel = curPixel.right;
    }

    // scans down until the pixel is in line width the y-coordinate
    for (int i = 0; i < y; i += 1) {
      curPixel = curPixel.down;
    }
    return curPixel;
  }

  // Finds the sentinel at the given row or column and at the given coordinate
  // coor is how many sentinels down or left the target sentinel is
  public APixel findSentinelAt(String side, int coor) {
    APixel curSen;
    
    if (side.equals("top") || side.equals("left")) {
      curSen = this.topleft;
    }
    else if (side.equals("bottom")) {
      curSen = this.bottomleft;
    }
    
    else if (side.equals("right")){
      curSen = this.topright;
    }
    else {
      throw new IllegalArgumentException("String supplied is not top, bottom, left, or right");
    }
    
    
    int i = 0;
    // scans left or down until the sentinel is in line with the given coordinate
    while (i < coor) {
      if (side.equals("top") || side.equals("bottom")) {
        curSen = curSen.right;
      }
      if (side.equals("left") || side.equals("right")) {
        curSen = curSen.down;
      }
      i += 1;
    }
    return curSen;
  }

  // return the leftmost pixel in the row, which is connected to all
  // all the other pixels in the row by right neighbors
  public Pixel createPixelRow(int y) {
    return createPixelRowRecur(0, y);
  }

  // helper method for createPixelRow. which returns the leftmost pixel
  // STARTING FROM the x-coordinate, which is connected to the rest of the row
  // to its right
  public Pixel createPixelRowRecur(int x, int y) {
    Pixel newPx = this.createPixel(x, y);

    // LEFT AND RIGHT NEIGHBORS

    // if the current pixel is all the way on the left, make the left pixel
    // a sentinel pixel from the left row of sentinels
    if (x == 0) {
      newPx.setLeft(this.findSentinelAt("left", y + 1));
    }
    // if the current pixel is all the way on the right, make the right pixel
    // a sentinel pixel from the right row of sentinels
    if (x == this.width - 1) {
      newPx.setRight(this.findSentinelAt("right", y + 1));

    }
    // if the current pixel is NOT all the way to the right, make the right pixel
    // the next recursion of createPixelRowRecur
    else {
      Pixel rightRow = createPixelRowRecur(x + 1, y);
      newPx.setRight(rightRow);
    }

    // UP AND DOWN NEIGHBORS

    // if you are currently building the bottom row, make the bottom pixel
    // a sentinel pixel from the bottom row of sentinels
    if (y == this.height - 1) {
      newPx.setDown(this.findSentinelAt("bottom", x + 1));
    }

    // if you are currently building the top row, make the top pixel
    // a sentinel pixel from the top row of sentinels

    if (y == 0) {
      newPx.setUp(this.findSentinelAt("top", x + 1));
    }

    // if you are NOT currently building the top row, make the top pixel
    // a pixel from the previous row

    else {
      newPx.setUp(this.findPixelAt(x, y - 1));
    }
    
    return newPx;
  }

  // creates the leftmost column and all of the rows that it is connected to,
  // thus also creating all the other columns
  // y represents how far down the leftmost column the function is currently at
  public APixel createPixelColumns(int y) {
    if (y == height - 1) {
      return createPixelRow(y);
    }
    else {
      Pixel newPx = this.createPixelRow(y);
      newPx.setDown(createPixelColumns(y + 1));
      return newPx;
    }
  }
  
  // creates the computed pixel image using the pixel graph already generated
  // _energyGrayscale_ determines if the cpi should show a grayscale to represent
  // the energy of every pixel
  // _seamGrayscale_ determines if the cpi should show a grayscale to represent
  // the most boring seam weight every pixel is connected to,
  // with that seam being vertical if _seamsVertical_ is true
  // and that seam being horizontal if _seamsVertical_ is false
  public ComputedPixelImage createCPI(boolean energyGrayscale, boolean seamGrayscale, boolean seamsVertical) {
    if (energyGrayscale && seamGrayscale) {
      throw new IllegalArgumentException
      ("A cpi cannot be generated to have both an energy grayscale and a seam grayscale");
    }
    
    ComputedPixelImage cpi = new ComputedPixelImage(this.width, this.height);
  
    int x = 0;
    int y = 0;
    
    // parses through every x and y coordinate in the pixel graph
    // and converts every coordinate pair to a pixel in the computed pixel image
    for (Iterable<APixel> row : this) {
      x = 0;
      Color c;
      for (APixel px : row) {
        c = this.createPixelColor(px, energyGrayscale, seamGrayscale, seamsVertical);
        cpi.setColorAt(x, y, c);
        x += 1;
      }
      y += 1;
    }
    return cpi;
  }
  
  // find the correct color that a pixel _px_ should be
  // _energyGrayscale_ determines if the pixel color should be a grayscale to represent
  // the energy of the pixel
  // _seamGrayscale_ determines if the pixel color should be a grayscale to represent
  // the most boring seam weight that the pixel is connected to,
  // with that seam being vertical if _seamsVertical_ is true
  // and that seam being horizontal if _seamsVertical_ is false
  public Color createPixelColor(APixel px, boolean energyGrayscale, boolean seamGrayscale, boolean seamsVertical) {
    if (energyGrayscale && seamGrayscale) {
      throw new IllegalArgumentException
      ("A color cannot be generated to have both an energy grayscale and a seam grayscale");
    }
    
    Color c;
    
    // if the energyGrayscale flag is on, set the color of the pixel its
    // corresponding energy grayscale color
    if (energyGrayscale) {
      c = px.getEnergyGrayscale();
    }
    
    // if the seamGrayscale flag is on, set the color of the pixel its
    // corresponding seam weight grayscale color according to whether seams
    // are being removed vertically or horizontally
    else if (seamGrayscale) {
      if (seamsVertical) {
        c = px.getSeamGrayscale(this.height, true);
      }
      else {
        c = px.getSeamGrayscale(this.width, false);
      }
    }
    
    // set the color of the pixel to its defaut color according to its 
    // rgb values
    else {
      // if this pixel has been flagged to be a part of a seam that is
      // about to be deleted, have it show up as red
      if (px.redPixel) {
        c = new Color(255, 0, 0);
      } else {
      c = new Color(px.red, px.green, px.blue);
      }
    }
    return c;
  }
  
  // Finds the most boring vertical seam in the image that goes all the way
  // from top to bottom
  public SeamInfo findMostBoringVertSeam() {
    ArrayList<SeamInfo> possibleSeams = new ArrayList<SeamInfo>();
    
    // parses through the entire bottom row to collect all of their most boring
    // seams, in order to find the most boring of them all.
    
    APixel blPixel = this.bottomleft.getTopRight();   
    IterableSubset iterRow = this.createIterableRow(blPixel);
    
      for (APixel curPixel : iterRow) {
        curPixel.addBoringVertSeamTo(possibleSeams);
        if (Objects.isNull(curPixel.boringVertSeam)) {
          throw new IllegalStateException("this pixel has a null boring vert seam");
        }
    }
    SeamInfo mostBoring = new Utils().minTotalWeight(possibleSeams);
    return mostBoring;
  }
  
  // Finds the most boring horizontal seam in the image that goes all the way
  // from left to right
  SeamInfo findMostBoringHorizSeam() {
    ArrayList<SeamInfo> possibleSeams = new ArrayList<SeamInfo>();
    
    // parses through the entire right column to collect all of their most boring
    // seams, in order to find the most boring of them all.
    
    APixel tr = this.topright.getDownLeft();   
    IterableSubset iterCol = this.createIterableCol(tr);
    
      for (APixel curPixel : iterCol) {
        curPixel.addBoringHorizSeamTo(possibleSeams);
        if (Objects.isNull(curPixel.boringHorizSeam)) {
          throw new IllegalStateException("this pixel has a null boring horiz seam");
        }
    }
      
    SeamInfo mostBoring = new Utils().minTotalWeight(possibleSeams);
    return mostBoring;
  }
  
  // fills the boringVertSeam field in all of the pixels of the current graph
  void generateVerticalSeams() {
    
    // parses through every x and y coordinate in the pixel graph,
    // starting from the top row and moving down once that top row is complete.
    // For every pixel, its most boring vertical seam is generated by looking at
    // the pixels above it (unless the pixel is currently in the top row)

    int y = 0;

    for (Iterable<APixel> row : this) {
      for (APixel curPixel : row) {

        // if the pixel is in the top row, generate a seam that only contains this pixel
        if (y == 0) {
          curPixel.boringVertSeam = new SeamInfo(curPixel);
        }
        else {
            curPixel.boringVertSeam = curPixel.findBoringVertSeam();
        }
      }
      y += 1;
    }
  }
  
  // fills the boringHorizSeam field in all of the pixels of the current graph
  void generateHorizontalSeams() {
    
    // parses through every x and y coordinate in the pixel graph,
    // starting from the left column and moving right once that left column is complete.
    // For every pixel, its most boring horizontal seam is generated by looking at
    // the pixels to the left of it (unless the pixel is currently in the left column)
    
    int x = 0;

    Iterator<Iterable<APixel>> colIter = this.iteratorCol();
    while(colIter.hasNext()) {
      Iterable<APixel> col = colIter.next();
      for (APixel curPixel : col) {
        
        // if the pixel is in the left column, generate a seam that only contains this pixel
        if (x == 0) {
          curPixel.boringHorizSeam = new SeamInfo(curPixel);
        }
        else {
          curPixel.boringHorizSeam = curPixel.findBoringHorizSeam();
        }
      }
      x += 1;
    }
  }

  // Finds if the entire graph of pixels (sentinels and normal pixels) is well
  // formed,
  // meaning that left and right neighbors agree are directed to each other,
  // up and down neighbors are directed to each other,
  // and a pixel can reach its diagonal neighbor two ways (up then right == right
  // then up)
  public boolean isWellFormed() {
    // checks if top and bottom sentinels are well formed
    for (int x = 1; x < width + 2; x += 1) {
      APixel topSenLeft = this.findSentinelAt("top", x - 1);
      APixel topSen = this.findSentinelAt("top", x);
      APixel topSenRight = this.findSentinelAt("top", x + 1);

      APixel botSenLeft = this.findSentinelAt("bottom", x - 1);
      APixel botSen = this.findSentinelAt("bottom", x);
      APixel botSenRight = this.findSentinelAt("bottom", x + 1);

      boolean topWellFormed = 
          topSenLeft.right == topSen
          && topSen.left == topSenLeft
          && topSenRight.left == topSen 
          && topSen.right == topSenRight;

      boolean botWellFormed = 
          botSenLeft.right == botSen 
          && botSen.left == botSenLeft
          && botSenRight.left == botSen 
          && botSen.right == botSenRight;

      if (!topWellFormed || !botWellFormed) {
        return false;
      }
    }

    // checks if left and right sentinels are well formed
    for (int y = 1; y < height + 2; y += 1) {
      APixel leftSenTop = this.findSentinelAt("left", y - 1);
      APixel leftSen = this.findSentinelAt("left", y);
      APixel leftSenBot = this.findSentinelAt("left", y + 1);

      APixel rightSenTop = this.findSentinelAt("right", y - 1);
      APixel rightSen = this.findSentinelAt("right", y);
      APixel rightSenBot = this.findSentinelAt("right", y + 1);

      boolean leftWellFormed = 
          leftSenTop.down == leftSen
          && leftSen.up == leftSenTop
          && leftSenBot.up == leftSen 
          && leftSen.down == leftSenBot;

      boolean rightWellFormed =
          rightSenTop.down == rightSen 
          && rightSen.up == rightSenTop
          && rightSenBot.up == rightSen
          && rightSen.down == rightSenBot;

      if (!leftWellFormed || !rightWellFormed) {
        return false;
      }
    }

    // checks if non-sentinel pixels are well formed
    for (int y = 2; y < height; y += 1) {
      for (int x = 2; x < width; x += 1) {
        APixel curPx = this.findPixelAt(x, y);
        if (!curPx.wellFormed()) {
          return false;
        }
      }
    }
    return true;
  }

  // Returns the default iterator of the world,
  // which iterates over every row in the world.
  public Iterator<Iterable<APixel>> iterator() {
    return new WorldIterRows(this);
  }
  
  // Returns the non-default iterator of the world,
  // which iterates over every column in the world.
  public Iterator<Iterable<APixel>> iteratorCol() {
    return new WorldIterCols(this);
  }
  
  // Creates an iterable set of APixels that represents a row in the world,
  // with the given _curPx_ being the leftmost pixel in that row
  public IterableSubset createIterableRow(APixel curPx) {
    ArrayList<APixel> contents = new ArrayList<APixel>();
    
    // until the current x-coordinate is one less than the width of the pixel grid,
    // add the pixel to the right of the current pixel to the end of the iterable set
    for (int x = 0; x < this.width; x += 1) {
      contents.add(curPx);
      curPx = curPx.right;
    }
    return new IterableSubset(contents);
  }
  
  // Creates an iterable set of APixels that represents a column in the world,
  // with the given _curPx_ being the topmost pixel in that column
  public IterableSubset createIterableCol(APixel curPx) {
    ArrayList<APixel> contents = new ArrayList<APixel>();
  
    // until the current y-coordinate is one less than the height of the pixel grid,
    // add the pixel that is below the current pixel to the end of the iterable set
    for (int y = 0; y < this.height; y += 1) {
      contents.add(curPx);
      curPx = curPx.down;
    }
    return new IterableSubset(contents);
  }
}

// Represents an iterable list of APixels where the order of the APixels matter
// These APixels could either make up row or a column
class IterableSubset implements Iterable<APixel> {
  ArrayList<APixel> contents; // the ordered list of APixels
  
  // Intializes the the ordered list of APixels to _contents_.
  IterableSubset(ArrayList<APixel> contents) {
    this.contents = contents;
  }

  // Returns the default iterator to iterate over this subset of APixels
  public Iterator<APixel> iterator() {
    return new SubsetIter(contents);
  }
}

// Represents an Iterator that will iterate over an ordered subset of APixels
class SubsetIter implements Iterator<APixel> {
  
  ArrayList<APixel> contents; // the ordered list of APixels

  // Intializes the the ordered list of APixels to _contents_.
  SubsetIter(ArrayList<APixel> contents) {
    this.contents = contents;
  }

  // determines if there is a next APixel in the current contents to return
  public boolean hasNext() {
    return contents.size() > 0;
  }
  
  // Returns the next APixel in the current contents
  // EFFECT removes the APixel at index 0 from the contents list
  public APixel next() {
    if (!this.hasNext()) {
      throw new IllegalArgumentException("Out of pixels in this subset.");
    }
    return contents.remove(0);
  }
}

// Represents an iterator that will iterate over every column of an ImageWorld
class WorldIterCols implements Iterator<Iterable<APixel>> {
  
  // The list of columns in the world begins as an empty list of columns
  ArrayList<Iterable<APixel>> cols = new ArrayList<Iterable<APixel>>();
  
  // Initializes the list of columns in the ImageWorld _w_
  WorldIterCols(ImageWorld w) {
    
    // beginning at the topleft most Pixel and parsing through the entire top row,
    // an iterable column is created using that pixel in the top row
    // and the column is added to the list of columns.
    APixel curPx = w.topleft.getDownRight();
    for (int x = 0; x < w.width; x += 1) {
      cols.add(w.createIterableCol(curPx));
      curPx = curPx.right;
    }  
  }
  
  // determines if there is a next column in the current list of columns to return
  public boolean hasNext() {
    return cols.size() > 0;
  }

  // Returns the next column in the current list of columns
  // EFFECT removes the column at index 0 from the columns list
  public Iterable<APixel> next() {
    if (!this.hasNext()) {
      throw new IllegalArgumentException("Out of columns");
    }
    return cols.remove(0);
  }
}

// Represents an iterator that will iterate over every rpw of an ImageWorld
class WorldIterRows implements Iterator<Iterable<APixel>> {
  
  // The list of rows in the world begins as an empty list of rows
  ArrayList<Iterable<APixel>> rows = new ArrayList<Iterable<APixel>>();
  
  // Initializes the list of rows in the ImageWorld _w_
  WorldIterRows(ImageWorld w) {
    // beginning at the topleft most Pixel and parsing through the entire left column,
    // an iterable row is created using that pixel in the left column
    // and the row is added to the list of rows.
    APixel curPx = w.topleft.getDownRight();
    for (int y = 0; y < w.height; y += 1) {
      rows.add(w.createIterableRow(curPx));
      curPx = curPx.down;
    }
  }

  // determines if there is a next row in the current list of rows to return
  public boolean hasNext() {
    return rows.size() > 0;
  }

  // Returns the next row in the current list of rows
  // EFFECT removes the row at index 0 from the row list
  public Iterable<APixel> next() {
    if (!this.hasNext()) {
      throw new IllegalArgumentException("Out of rows");
    }
    return rows.remove(0);
  }
}

// represents a pixel in the graph, either a sentinel or normal pixel
abstract class APixel {

  APixel up; // the pixel above this one

  APixel left; // the pixel to the left of this one
  APixel right; // the pixel to the right of this one

  APixel down; // the pixel below this one

  int red; // the red value in this pixel's rgb color set
  int green; // the green value in this pixel's rgb color set
  int blue; // the blue value in this pixel's rgb color set

  // the most boring seam that grown up from this current pixel
  SeamInfo boringVertSeam;
  
  // the most boring seam that grown left from this current pixel
  SeamInfo boringHorizSeam;
  
  // Is this pixel in a seam that is about to be deleted, and therefore should show up as red?
  boolean redPixel;

  // initializes the rgb comonents of this pixel
  APixel(int red, int green, int blue) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    
    this.redPixel = false;

  }

  // Finds if the pixel is well formed,
  // meaning that left and right neigbors agree are directed to each other,
  // up and down nighbors are directed to each other,
  // and a pixel can reach its diagonal neighbor two ways (up then right == right
  // then up)
  abstract boolean wellFormed();

  // finds the most boring seam that grown up from this current pixel
  // by attaching this pixel to the most boring seam out of all it's above
  // neighbors
  // (topleft, top, topright)
  abstract SeamInfo findBoringVertSeam();
  
  // finds the most boring seam that grows left from this current pixel
  // by attaching this pixel to the most boring seam out of all it's left
  // neighbors
  // (topleft, left, downleft)
  abstract SeamInfo findBoringHorizSeam();

  // finds the brightness of the current pixel,
  // defined as the average of its red, green, and blue components divided by 255.0
  // results close to 0 are dark, and results close to 1 are light
  public double brightness() {
    return (this.red + this.green + this.blue) / 765.0;
  }

  // adds the boring seam that this current pixel is connected to, to the given
  // list
  abstract ArrayList<SeamInfo> addBoringVertSeamTo(ArrayList<SeamInfo> list);
  
  // adds the boring seam that this current pixel is connected to, to the given
  // list
  abstract ArrayList<SeamInfo> addBoringHorizSeamTo(ArrayList<SeamInfo> list);

  // calculates how bright this pixel is in comparison to its neighbors
  abstract double energy();

  // given the other pixel that is supposed to be above this pixel,
  // sets this pixel's up value and the other pixel's down value
  public void setUp(APixel up) {
    this.up = up;
    up.down = this;
  }

  // given the other pixel that is supposed to be to the left of this pixel,
  // sets this pixel's left value and the other pixel's right value
  public void setLeft(APixel left) {
    this.left = left;
    left.right = this;
  }

  // given the other pixel that is supposed to be to the right of this pixel,
  // sets this pixel's right value and the other pixel's left value
  public void setRight(APixel right) {
    this.right = right;
    right.left = this;
  }

  // given the other pixel that is supposed to be below this pixel,
  // sets this pixel's down value and the other pixel's up value
  public void setDown(APixel down) {
    this.down = down;
    down.up = this;
  }

  // returns the pixel that is diagonally top left to this pixel
  public APixel getTopLeft() {
    return this.up.left;
  }

  // returns the pixel that is diagonally top right to this pixel
  public APixel getTopRight() {
    return this.up.right;
  }

  // returns the pixel that is diagonally bottom left to this pixel
  public APixel getDownLeft() {
    return this.down.left;
  }

  // returns the pixel that is diagonally bottom right to this pixel
  public APixel getDownRight() {
    return this.down.right;
  }
  
  // Finds the color of this current pixel as a scale of its energy,
  // where the minimum energy is black and the maximum energy is white
  public Color getEnergyGrayscale() {
    int energyGrayscale = (int) ((this.energy() / 4.0) * 255);
    Color c = new Color(energyGrayscale, energyGrayscale, energyGrayscale);
    return c;
  }
  
  // Finds the color of this current pixel as a scale of its the totalWeight
  // of the most boring seam it is connected to, either in the vertical or horizontal direction.
  // The minimum seam weight is black and the maximum seam weight is white
  public Color getSeamGrayscale(double worldDimension, boolean vertical) {
    Color c;
    // Once we find the seam weight as a decimal between 0 and 1, 
    // our results are too close together because the majority
    // of the weights would fall near the 0 mark. So, we raise our reult to the power of 2/5.
    // The makes it so the resultant range is still from 0 to 1, but the grayscale is more spread out
    // and the image has more contrast to it.
    if (vertical) {
      int seamGrayscale = (int) (Math.pow(this.boringVertSeam.totalWeight / (4.0 * worldDimension), (1.0/2.5)) * 255.0);
      c = new Color(seamGrayscale, seamGrayscale, seamGrayscale);
    }
    else {
      int seamGrayscale = (int) (Math.pow(this.boringHorizSeam.totalWeight / (4.0 * worldDimension), (1.0/2.5)) * 255.0);
      c = new Color(seamGrayscale, seamGrayscale, seamGrayscale);
    }
    return c;
  }
  
  // deletes the given pixel located at the row y,
  // and adjusts its neighbors to keep the graph well formed
  void deletePixelVert(int y, int height) {
    this.right.setLeft(this.left);

    // if the pixel that needs to be deleted is on the bottom row
    if (y == height - 1) {
      // delete the sentinel below the current pixel
      this.getDownRight().setLeft(this.getDownLeft());
    }
    // if the pixel that needs to be deleted is on the top row
    if (y == 0) {
      // delete the sentinel above the current pixel
      this.getTopRight().setLeft(this.getTopLeft());
    }
    // if the pixel that needs to be deleted is NOT on the top row
    else {
      if (this.boringVertSeam.cameFrom.px == this.getTopLeft()) {
        this.up.setDown(this.left);
      }
      if (this.boringVertSeam.cameFrom.px == this.getTopRight()) {
        this.up.setDown(this.right);
      }
    }
  }
  
  // deletes the given pixel located at the column x,
  // and adjusts its neighbors to keep the graph well formed
  void deletePixelHoriz(int x, int width) {
    this.down.setUp(this.up);

    // if the pixel that needs to be deleted is on the right column
    if (x == width - 1) {
      // delete the sentinel to the right of the current pixel
      this.getDownRight().setUp(this.getTopRight());
    }
    // if the pixel that needs to be deleted is on the left column
    if (x == 0) {
      // delete the sentinel to the left of the current pixel
      this.getDownLeft().setUp(this.getTopLeft());

    }
    // if the pixel that needs to be deleted is NOT on the right or left column
    else { 
      if (this.boringHorizSeam.cameFrom.px == this.getTopLeft()) {
        this.up.setLeft(this.left);
      }
      if (this.boringHorizSeam.cameFrom.px == this.getDownLeft()) {
        this.down.setLeft(this.left);
      }
    }
  }
  
 // adds the given pixel located at the row y,
 // and adjusts its neighbors to keep the graph well formed
 void addPixelVert(int y, int height) {
   this.redPixel = false;
   this.right.setLeft(this);
   this.left.setRight(this);

   // if the pixel that needs to be added is on the bottom row
   if (y == height - 1) {
     // add the sentinel below the current pixel
     this.getDownRight().setLeft(this.down);
     this.getDownLeft().setRight(this.down);
   }
   // if the pixel that needs to be added is on the top row
   if (y == 0) {
     // add the sentinel above the current pixel
     this.getTopRight().setLeft(this.up);
     this.getTopLeft().setRight(this.up);
   }
   // if the pixel that needs to be added is NOT on the top row
   else {
     this.up.setDown(this);
     this.down.setUp(this);
   }
 }
 
 // adds the given pixel located at the column x,
 // and adjusts its neighbors to keep the graph well formed
 void addPixelHoriz(int x, int width) {
   this.redPixel = false;
   this.down.setUp(this);
   this.up.setDown(this);

   // if the pixel that needs to be added is on the right column
   if (x == width - 1) {
     // add the sentinel to the right of the current pixel
     this.getDownRight().setUp(this.right);
     this.getTopRight().setDown(this.right);
   }
   // if the pixel that needs to be added is on the left column
   if (x == 0) {
     // add the sentinel to the left of the current pixel
     this.getDownLeft().setUp(this.left);
     this.getTopLeft().setDown(this.left);

   }
   // if the pixel that needs to be added is NOT on the right or left column
   else { 
     this.right.setLeft(this);
     this.left.setRight(this);
   }
 }
}

// A sentinel is a border pixel that does not show up on screen 
// and does not exhibit a color of the shown image
class Sentinel extends APixel {

  // initializes the sentinel to be a black pixel
  Sentinel() {
    super(0, 0, 0);
  }

  // a sentinel cannot be added to a seam, so return the list
  public ArrayList<SeamInfo> addBoringVertSeamTo(ArrayList<SeamInfo> list) {
    return list;
  }
  
  // a sentinel cannot be added to a seam, so return the list
  public ArrayList<SeamInfo> addBoringHorizSeamTo(ArrayList<SeamInfo> list) {
    return list;
  }

  // a sentinel cannot be used to find a boring vertical seam, so throws an exception
  public SeamInfo findBoringVertSeam() {
    throw new IllegalStateException();
  }
  
  // a sentinel cannot be used to find a boring horizontal seam, so throws an exception
  public SeamInfo findBoringHorizSeam() {
    throw new IllegalStateException();
  }

  // calculates how bright the sentinel is compared to its neighbors
  // the sentinel will always have an energy of 0
  public double energy() {
    return 0;
  }
  
  // Finds if the pixel is well formed,
  // meaning that left and right neigbors agree are directed to each other,
  // up and down nighbors are directed to each other,
  // and a pixel can reach its diagonal neighbor two ways (up then right == right
  // then up)
  
  // If this method was called on a sentinel, there was an error. Sentienls are
  // checked for well-formedness in the world's well-formed method.
  boolean wellFormed() {
    throw new IllegalStateException("wellFormed() can't be called on a sentinel");
  } 
}

// Represents a pixel that help make up the image shown on screen
class Pixel extends APixel {

  // initializes the rgb values of the pixel
  Pixel(int red, int green, int blue) {
    super(red, green, blue);
  }

  // Finds how different the brightness on the left is to the brightness on the right
  // The larger the energy, the larger the difference
  public double horizEnergy() {
    double tl_br = this.getTopLeft().brightness();
    double left_br = this.left.brightness();
    double dl_br = this.getDownLeft().brightness();
    double tr_br = this.getTopRight().brightness();
    double right_br = this.right.brightness();
    double dr_br = this.getDownRight().brightness();

    return (tl_br + (2 * left_br) + dl_br) - (tr_br + (2 * right_br) + dr_br);
  }

  // Finds how different the brightness on the top is to the brightness on the bottom
  // The larger the energy, the larger the difference
  public double vertEnergy() {
    double tl_br = this.getTopLeft().brightness();
    double up_br = this.up.brightness();
    double tr_br = this.getTopRight().brightness();
    double dl_br = this.getDownLeft().brightness();
    double down_br = this.down.brightness();
    double dr_br = this.getDownRight().brightness();

    return (tl_br + (2 * up_br) + tr_br) - (dl_br + (2 * down_br) + dr_br);
  }

  // Finds how different the brightnesses all neighbors of this pixel are to each other
  // The larger the energy, the larger the difference
  double energy() {
    return Math.sqrt(Math.pow(this.horizEnergy(), 2) + Math.pow(this.vertEnergy(), 2));
  }

  // Finds the most boring vertical seam that is connected to this pixel
  public SeamInfo findBoringVertSeam() {
    ArrayList<SeamInfo> possibleCameFroms = new ArrayList<SeamInfo>();

    // we are using a call to addBoringVertSeamTo, because that method adds seams to
    // a list have already been generated. Since we are moving from the top down,
    // all of the pixels in the row above this one have their vertical seams generated.
    // So, we can access already generated seams without re-calculating them.
    
    // there are three possibilities to generate a boring vertical seam
    this.getTopLeft().addBoringVertSeamTo(possibleCameFroms); // top left neighbor
    this.up.addBoringVertSeamTo(possibleCameFroms); // directly above neighbor
    this.getTopRight().addBoringVertSeamTo(possibleCameFroms); // top right neighbor

    // finds the most boring of those three vertical seams
    SeamInfo newCameFrom = new Utils().minTotalWeight(possibleCameFroms);

    // creates a new seam with the most boring vertical seam
    return new SeamInfo(this, newCameFrom.totalWeight + this.energy(), newCameFrom);
  }
  
  // Finds the most boring horizontal seam that is connected to this pixel
  public SeamInfo findBoringHorizSeam() {
    ArrayList<SeamInfo> possibleCameFroms = new ArrayList<SeamInfo>();
    
    // we are using a call to addBoringHorizSeamTo, because that method adds seams to
    // a list have already been generated. Since we are moving from the left to the right,
    // all of the pixels in the column to the left this one have their horizontal seams generated.
    // So, we can access already generated seams without re-calculating them.

    // there are three possibilities to generate a boring vertical seam
    this.getTopLeft().addBoringHorizSeamTo(possibleCameFroms); // top left neighbor
    this.left.addBoringHorizSeamTo(possibleCameFroms); // directly left neighbor
    this.getDownLeft().addBoringHorizSeamTo(possibleCameFroms); // down left neighbor

    // finds the most boring of those three vertical seams
    SeamInfo newCameFrom = new Utils().minTotalWeight(possibleCameFroms);

    // creates a new seam with the most boring vertical seam
    return new SeamInfo(this, newCameFrom.totalWeight + this.energy(), newCameFrom);
  }

  // adds a boring vertical seam to the given list
  public ArrayList<SeamInfo> addBoringVertSeamTo(ArrayList<SeamInfo> list) {
    list.add(this.boringVertSeam);
    return list;
  }
  
  // adds a boring vertical seam to the given list
  public ArrayList<SeamInfo> addBoringHorizSeamTo(ArrayList<SeamInfo> list) {
    if (this.boringHorizSeam == null) {
      throw new IllegalStateException("trying to access a null seam");
    }
    list.add(this.boringHorizSeam);
    return list;
  }

  // Finds if the pixel is well formed,
  // meaning that left and right neigbors agree are directed to each other,
  // up and down nighbors are directed to each other,
  // and a pixel can reach its diagonal neighbor two ways (up then right == right
  // then up)
  public boolean wellFormed() {
    // basic well-formed tests
    boolean sameAbove = this.up.down == this;
    boolean sameBelow = this.down.up == this;
    boolean sameLeft = this.left.right == this;
    boolean sameRight = this.right.left == this;
    
    // topLeft neighbor equivalences
    boolean topLeft_LeftPx = this.getTopLeft() == this.left.up;
    boolean topLeft_UpPx = this.getTopLeft() == this.up.left;
    
    // topRight neighbor equivalences
    boolean topRight_RightPx = this.getTopRight() == this.right.up;
    boolean topRight_UpPx = this.getTopRight() == this.up.right;
    
    // bottomLeft neighbor equivalences
    boolean bottomLeft_LeftPx = this.getDownLeft() == this.left.down;
    boolean bottomLeft_DownPx = this.getDownLeft() == this.down.left;
    
    // bottomRight neighbor equivalences
    boolean bottomRight_RightPx = this.getDownRight() == this.right.down;
    boolean bottomRight_DownPx = this.getDownRight() == this.down.right;
    
    return sameAbove && sameBelow && sameLeft && sameRight
        && topLeft_LeftPx 
        && topLeft_UpPx
        && topRight_RightPx 
        && topRight_UpPx
        && bottomLeft_LeftPx
        && bottomLeft_DownPx
        && bottomRight_RightPx 
        && bottomRight_DownPx;
  }
}

// Contains information regarding a horizontal and vertical seam,
// ending at the downmost or rightmost pixel _px_.
class SeamInfo {
  APixel px; // the current pixel in the seam
  double totalWeight; // the total weight of the seam so far
  SeamInfo cameFrom; // the seam built up until this point

  // Main Constructor: Initializes the current pixel _px_,
  // the total weight so far _totalWeight_,
  // and the seam so far _cameFrom_.
  SeamInfo(APixel px, double totalWeight, SeamInfo cameFrom) {
    this.px = px;
    this.totalWeight = totalWeight;
    this.cameFrom = cameFrom;
  }

  // Base Case: Initializes the current pixel _px_ with a pixel from the top row
  // of the image,
  // sets the totalWeight to the current pixel's weight,
  // and keeps cameFrom at null
  SeamInfo(APixel px) {
    this(px, px.energy(), null);
  }

  // deletes this seam from its surroundings, starting at the given y-coordinate
  public void deleteSeamVert(int y, int height) {
    px.deletePixelVert(y, height);
    if (!Objects.isNull(this.cameFrom)) {
      cameFrom.deleteSeamVert(y - 1, height);
    }
  }
  
  // deletes this seam from its surroundings, starting at the given x-coordinate
  public void deleteSeamHoriz(int x, int width) {
    px.deletePixelHoriz(x, width);
    if (!Objects.isNull(this.cameFrom)) {
      cameFrom.deleteSeamHoriz(x - 1, width);
    }
  }
  
  // adds seam from into given world, starting at the given y-coordinate
  public void addSeamVert(int y, int height) {
    px.addPixelVert(y, height);
    if (!Objects.isNull(this.cameFrom)) {
      cameFrom.addSeamVert(y - 1, height);
    }
  }
  
  // deletes this seam from a given world, starting at the given x-coordinate
  public void addSeamHoriz(int x, int width) {
    px.addPixelHoriz(x, width);
    if (!Objects.isNull(this.cameFrom)) {
      cameFrom.addSeamHoriz(x - 1, width);
    }
  }

  // Checks to see if this seam is more boring (has less totalWeight) than a given seam
  public boolean moreBoringSeam(SeamInfo other) {
    return this.totalWeight < other.totalWeight;
  }
  
  // makes all the pixels belonging to the given seam red
  void paintSeamRed() {
    this.px.redPixel = true;
    if (this.cameFrom != null) {
      this.cameFrom.paintSeamRed();
    }
  }
}

// examples class that contains object examples and method tests
class ExamplesSeamCarving {
  Pixel pxRed;
  Pixel pxGreen;
  Pixel pxBlue;
  Pixel pxWhite;
  Pixel pxBlack;
  Pixel pxPurple;
  Pixel pxYellow;
  Pixel pxOrange;
  Pixel pxGrey;
  
  FromFileImage tree;
  ImageWorld treeWorldVertical;
  ImageWorld treeWorldHorizontal;
  BigBangWorld bbTreeWorldVertical;
  BigBangWorld bbTreeWorldHorizontal;
  
  FromFileImage ben;
  ImageWorld benWorldHorizontal;
  BigBangWorld bbBenWorldHorizontal;
  
  FromFileImage balloons;
  ImageWorld balloonsWorldVertical;
  ImageWorld balloonsWorldHorizontal;
  BigBangWorld bbBalloonsWorldVertical;
  BigBangWorld bbBalloonsWorldHorizontal;

  APixel vTopLeft;
  APixel vTop1;
  APixel vTop2;
  APixel vTop3;

  APixel vTopRight;
  APixel vRight1;
  APixel vRight2;
  APixel vRight3;

  APixel vBottomRight;

  APixel vBottomLeft;

  APixel vBottom1;
  APixel vBottom2;
  APixel vBottom3;

  APixel vLeft1;
  APixel vLeft2;
  APixel vLeft3;

  APixel vPx00;
  APixel vPx10;
  APixel vPx20;

  APixel vPx01;
  APixel vPx11;
  APixel vPx21;

  APixel vPx02;
  APixel vPx12;
  APixel vPx22;
  
  APixel hTopLeft;
  APixel hTop1;
  APixel hTop2;
  APixel hTop3;

  APixel hTopRight;
  APixel hRight1;
  APixel hRight2;
  APixel hRight3;

  APixel hBottomRight;

  APixel hBottomLeft;

  APixel hBottom1;
  APixel hBottom2;
  APixel hBottom3;

  APixel hLeft1;
  APixel hLeft2;
  APixel hLeft3;

  APixel hPx00;
  APixel hPx10;
  APixel hPx20;

  APixel hPx01;
  APixel hPx11;
  APixel hPx21;

  APixel hPx02;
  APixel hPx12;
  APixel hPx22;

  SeamInfo vSeam00;
  SeamInfo vSeam10;
  SeamInfo vSeam20;

  SeamInfo vSeam01;
  SeamInfo vSeam11;
  SeamInfo vSeam21;

  SeamInfo vSeam02;
  SeamInfo vSeam12;
  SeamInfo vSeam22;
  
  SeamInfo hSeam00;
  SeamInfo hSeam10;
  SeamInfo hSeam20;

  SeamInfo hSeam01;
  SeamInfo hSeam11;
  SeamInfo hSeam21;

  SeamInfo hSeam02;
  SeamInfo hSeam12;
  SeamInfo hSeam22;


  void init() {

    tree = new FromFileImage("tree.jpg");
    treeWorldVertical = new ImageWorld(tree, true);
    treeWorldHorizontal = new ImageWorld(tree, false);
    bbTreeWorldVertical = new BigBangWorld(treeWorldVertical, true);
    bbTreeWorldHorizontal = new BigBangWorld(treeWorldHorizontal, false);
    
    pxRed = new Pixel(255, 0, 0);
    pxGreen = new Pixel(0, 255, 0);
    pxBlue = new Pixel(0, 0, 255);
 
    pxWhite = new Pixel(255,255,255);
    pxBlack = new Pixel(0,0,0);
    pxPurple = new Pixel(255, 0, 255);
    pxYellow = new Pixel(255,255,0);
    pxOrange = new Pixel(255,128,0);
    pxGrey = new Pixel(128,128,128);

    vTopLeft = new Sentinel();
    vTop1 = new Sentinel();
    vTop2 = new Sentinel();
    vTop3 = new Sentinel();

    vTopRight = new Sentinel();
    vRight1 = new Sentinel();
    vRight2 = new Sentinel();
    vRight3 = new Sentinel();

    vBottomRight = new Sentinel();

    vBottomLeft = new Sentinel();
    vBottom1 = new Sentinel();
    vBottom2 = new Sentinel();
    vBottom3 = new Sentinel();

    vLeft1 = new Sentinel();
    vLeft2 = new Sentinel();
    vLeft3 = new Sentinel();

    vTopLeft.setRight(vTop1);
    vTop1.setRight(vTop2);
    vTop2.setRight(vTop3);
    vTop3.setRight(vTopRight);
    vTopRight.setRight(vTopLeft);

    vTopLeft.setUp(vBottomLeft);
    vTop1.setUp(vBottom1);
    vTop2.setUp(vBottom2);
    vTop3.setUp(vBottom3);

    vTopRight.setUp(vBottomRight);

    vTopRight.setDown(vRight1);
    vRight1.setDown(vRight2);
    vRight2.setDown(vRight3);
    vRight3.setDown(vBottomRight);

    vRight1.setRight(vLeft1);
    vRight2.setRight(vLeft2);
    vRight3.setRight(vLeft3);

    vBottomRight.setRight(vBottomLeft);

    vBottomLeft.setRight(vBottom1);
    vBottom1.setRight(vBottom2);
    vBottom2.setRight(vBottom3);
    vBottom3.setRight(vBottomRight);

    vTopLeft.setDown(vLeft1);
    vLeft1.setDown(vLeft2);
    vLeft2.setDown(vLeft3);
    vLeft3.setDown(vBottomLeft);

    vPx00 = new Pixel(135, 101, 128);
    vPx10 = new Pixel(124, 90, 117);
    vPx20 = new Pixel(127, 78, 110);

    vPx01 = new Pixel(168, 134, 161);
    vPx11 = new Pixel(148, 114, 141);
    vPx21 = new Pixel(139, 90, 122);

    vPx02 = new Pixel(191, 169, 192);
    vPx12 = new Pixel(170, 148, 171);
    vPx22 = new Pixel(156, 120, 148);

    vPx00.setLeft(vLeft1);
    vPx00.setUp(vTop1);
    vPx00.setRight(vPx10);

    vPx10.setUp(vTop2);
    vPx10.setRight(vPx20);

    vPx20.setUp(vTop3);
    vPx20.setRight(vRight1);

    vPx01.setLeft(vLeft2);
    vPx01.setUp(vPx00);
    vPx01.setRight(vPx11);

    vPx11.setUp(vPx10);
    vPx11.setRight(vPx21);

    vPx21.setUp(vPx20);
    vPx21.setRight(vRight2);

    vPx02.setLeft(vLeft3);
    vPx02.setUp(vPx01);
    vPx02.setRight(vPx12);
    vPx02.setDown(vBottom1);

    vPx12.setUp(vPx11);
    vPx12.setRight(vPx22);
    vPx12.setDown(vBottom2);

    vPx22.setUp(vPx21);
    vPx22.setRight(vRight3);
    vPx22.setDown(vBottom3);
    
    hTopLeft = new Sentinel();
    hTop1 = new Sentinel();
    hTop2 = new Sentinel();
    hTop3 = new Sentinel();

    hTopRight = new Sentinel();
    hRight1 = new Sentinel();
    hRight2 = new Sentinel();
    hRight3 = new Sentinel();

    hBottomRight = new Sentinel();

    hBottomLeft = new Sentinel();
    hBottom1 = new Sentinel();
    hBottom2 = new Sentinel();
    hBottom3 = new Sentinel();

    hLeft1 = new Sentinel();
    hLeft2 = new Sentinel();
    hLeft3 = new Sentinel();

    hTopLeft.setRight(hTop1);
    hTop1.setRight(hTop2);
    hTop2.setRight(hTop3);
    hTop3.setRight(hTopRight);
    hTopRight.setRight(hTopLeft);

    hTopLeft.setUp(hBottomLeft);
    hTop1.setUp(hBottom1);
    hTop2.setUp(hBottom2);
    hTop3.setUp(hBottom3);

    hTopRight.setUp(hBottomRight);

    hTopRight.setDown(hRight1);
    hRight1.setDown(hRight2);
    hRight2.setDown(hRight3);
    hRight3.setDown(hBottomRight);

    hRight1.setRight(hLeft1);
    hRight2.setRight(hLeft2);
    hRight3.setRight(hLeft3);

    hBottomRight.setRight(hBottomLeft);

    hBottomLeft.setRight(hBottom1);
    hBottom1.setRight(hBottom2);
    hBottom2.setRight(hBottom3);
    hBottom3.setRight(hBottomRight);

    hTopLeft.setDown(hLeft1);
    hLeft1.setDown(hLeft2);
    hLeft2.setDown(hLeft3);
    hLeft3.setDown(hBottomLeft);

    hPx00 = new Pixel(135, 101, 128);
    hPx10 = new Pixel(124, 90, 117);
    hPx20 = new Pixel(127, 78, 110);

    hPx01 = new Pixel(168, 134, 161);
    hPx11 = new Pixel(148, 114, 141);
    hPx21 = new Pixel(139, 90, 122);

    hPx02 = new Pixel(191, 169, 192);
    hPx12 = new Pixel(170, 148, 171);
    hPx22 = new Pixel(156, 120, 148);

    hPx00.setLeft(hLeft1);
    hPx00.setUp(hTop1);
    hPx00.setRight(hPx10);

    hPx10.setUp(hTop2);
    hPx10.setRight(hPx20);

    hPx20.setUp(hTop3);
    hPx20.setRight(hRight1);

    hPx01.setLeft(hLeft2);
    hPx01.setUp(hPx00);
    hPx01.setRight(hPx11);

    hPx11.setUp(hPx10);
    hPx11.setRight(hPx21);

    hPx21.setUp(hPx20);
    hPx21.setRight(hRight2);

    hPx02.setLeft(hLeft3);
    hPx02.setUp(hPx01);
    hPx02.setRight(hPx12);
    hPx02.setDown(hBottom1);

    hPx12.setUp(hPx11);
    hPx12.setRight(hPx22);
    hPx12.setDown(hBottom2);

    hPx22.setUp(hPx21);
    hPx22.setRight(hRight3);
    hPx22.setDown(hBottom3);
    
    vSeam00 = new SeamInfo(vPx00);
    vSeam10 = new SeamInfo(vPx10);
    vSeam20 = new SeamInfo(vPx20);

    vSeam01 = new SeamInfo(vPx01, 4.372543666003845, vSeam10);
    vSeam11 = new SeamInfo(vPx11, 2.9636493636177907, vSeam20);
    vSeam21 = new SeamInfo(vPx21, 4.187699090971817, vSeam20);

    vSeam02 = new SeamInfo(vPx02, 5.4690246775599025, vSeam11);
    vSeam12 = new SeamInfo(vPx12, 5.135246490367836, vSeam11);
    vSeam22 = new SeamInfo(vPx22, 5.275633494707401, vSeam11);

    vPx00.boringVertSeam = vSeam00;
    vPx10.boringVertSeam = vSeam10;
    vPx20.boringVertSeam = vSeam20;

    vPx01.boringVertSeam = vSeam01;
    vPx11.boringVertSeam = vSeam11;
    vPx21.boringVertSeam = vSeam21;

    vPx02.boringVertSeam = vSeam02;
    vPx12.boringVertSeam = vSeam12;
    vPx22.boringVertSeam = vSeam22;
    
    
    hSeam00 = new SeamInfo(hPx00);
    hSeam01 = new SeamInfo(hPx01);
    hSeam02 = new SeamInfo(hPx02);

    hSeam10 = new SeamInfo(hPx10, 4.361607660412005, hSeam00);
    hSeam11 = new SeamInfo(hPx11, 3.1837710313897327, hSeam00);
    hSeam12 = new SeamInfo(hPx12, 4.408775555097109, hSeam01);

    hSeam20 = new SeamInfo(hPx20, 5.189891786373014, hSeam11);
    hSeam21 = new SeamInfo(hPx21, 5.365349367378268, hSeam11);
    hSeam22 = new SeamInfo(hPx22, 5.495755162479344, hSeam11);

    hPx00.boringHorizSeam = hSeam00;
    hPx10.boringHorizSeam = hSeam10;
    hPx20.boringHorizSeam = hSeam20;

    hPx01.boringHorizSeam = hSeam01;
    hPx11.boringHorizSeam = hSeam11;
    hPx21.boringHorizSeam = hSeam21;

    hPx02.boringHorizSeam = hSeam02;
    hPx12.boringHorizSeam = hSeam12;
    hPx22.boringHorizSeam = hSeam22;
  }
  
  void initAllTestWorlds() {
    tree = new FromFileImage("tree.jpg");
    treeWorldVertical = new ImageWorld(tree, true);
    treeWorldHorizontal = new ImageWorld(tree, false);
    bbTreeWorldVertical = new BigBangWorld(treeWorldVertical, true);
    bbTreeWorldHorizontal= new BigBangWorld(treeWorldHorizontal, false);

    balloons = new FromFileImage("balloons.jpg");
    balloonsWorldVertical = new ImageWorld(balloons, true);
    balloonsWorldHorizontal = new ImageWorld(balloons, false);
    bbBalloonsWorldVertical = new BigBangWorld(balloonsWorldVertical, true);
    bbBalloonsWorldHorizontal = new BigBangWorld(balloonsWorldHorizontal, false);

    
    ben = new FromFileImage("ben_lerner.jpg");
    benWorldHorizontal = new ImageWorld(ben, false);
    bbBenWorldHorizontal = new BigBangWorld(benWorldHorizontal, false);
    
  }
  
  void testDeleteSeamVertical(Tester t) {
    this.init();
    
    bbTreeWorldVertical.onTick();
    bbTreeWorldVertical.onTick();

    t.checkExpect(treeWorldVertical.isWellFormed(), true);
    
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).red, 135);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).green, 101);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).blue, 128);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).blue, 117);
    
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).red, 168);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).green, 134);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).blue, 161);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).red, 139);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).green, 90);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).blue, 122);

    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).red, 191);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).green, 169);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).blue, 192);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).red, 156);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).green, 120);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).blue, 148);

  }
 
  void testDeleteSeamHorizontal(Tester t) {
    this.init();
    
    bbTreeWorldHorizontal.onTick();
    bbTreeWorldHorizontal.onTick();

    t.checkExpect(treeWorldHorizontal.isWellFormed(), true);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 161);

    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);

    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 191);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 169);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 192);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 170);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 171);

    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).red, 139);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).blue, 122);
  
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).red, 156);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).green, 120);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).blue, 148);
  }

  void testWellFormedWorld(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.isWellFormed(), true);
    
    this.init();
    treeWorldVertical.topleft.left = pxRed;
    t.checkExpect(treeWorldVertical.isWellFormed(), false);
  }
  
  void testWellFormedPixel(Tester t) {
    this.init();
    pxRed.setRight(pxGreen);
    pxGreen.setRight(pxBlue);
    pxWhite.setUp(pxRed);
    pxWhite.setRight(pxBlack);
    pxBlack.setUp(pxGreen);
    pxBlack.setRight(pxPurple);
    pxPurple.setUp(pxBlue);
    pxYellow.setUp(pxWhite);
    pxYellow.setRight(pxOrange);
    pxOrange.setUp(pxBlack);
    pxOrange.setRight(pxGrey);
    pxGrey.setUp(pxPurple);
    
    t.checkExpect(pxBlack.wellFormed(), true);
    
    pxBlack.left = pxGrey;
    t.checkExpect(pxBlack.wellFormed(), false);
  }

  void testFindMostBoringVertSeam(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findMostBoringVertSeam(), vSeam12);
  }

  void testFindMostBoringHorizSeam(Tester t) {
    this.init();
    t.checkExpect(treeWorldHorizontal.findMostBoringHorizSeam(), hSeam20);
  }

  void testGenerateVerticalSeams(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2).boringVertSeam, vSeam22);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).boringVertSeam, vSeam01);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).boringVertSeam, vSeam00);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2).boringVertSeam, vSeam22);
  }
 
  void testGenerateHorizontalSeams(Tester t) {
    this.init();
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 2).boringHorizSeam, hSeam22);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).boringHorizSeam, hSeam01);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).boringHorizSeam, hSeam00);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 2).boringHorizSeam, hSeam22);
  }

  void testMoreBoringSeam(Tester t) {
    this.init();
    t.checkExpect(vSeam12.moreBoringSeam(vSeam22), true);
    t.checkExpect(vSeam01.moreBoringSeam(vSeam21), false);
  }

  void testMinTotalWeight(Tester t) {
    this.init();
    ArrayList<SeamInfo> row0 = new ArrayList<SeamInfo>();
    row0.add(vSeam00);
    row0.add(vSeam10);
    row0.add(vSeam20);

    ArrayList<SeamInfo> row1 = new ArrayList<SeamInfo>();
    row1.add(vSeam01);
    row1.add(vSeam11);
    row1.add(vSeam21);

    ArrayList<SeamInfo> row2 = new ArrayList<SeamInfo>();
    row2.add(vSeam02);
    row2.add(vSeam12);
    row2.add(vSeam22);

    Utils utils = new Utils();
    t.checkExpect(utils.minTotalWeight(row0), vSeam20);
    t.checkExpect(utils.minTotalWeight(row1), vSeam11);
    t.checkExpect(utils.minTotalWeight(row2), vSeam12);
  }

  void testAddBoringVertSeamTo(Tester t) {
    this.init();

    ArrayList<SeamInfo> seams = new ArrayList<SeamInfo>();
    ArrayList<SeamInfo> seamsWith20 = new ArrayList<SeamInfo>();
    seamsWith20.add(vSeam20);

    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).addBoringVertSeamTo(seams),
        new ArrayList<SeamInfo>());
    t.checkExpect(treeWorldVertical.findPixelAt(2, 0).addBoringVertSeamTo(seams), seamsWith20);
  }
  
  void testAddBoringHorizSeamTo(Tester t) {
    this.init();

    ArrayList<SeamInfo> seams = new ArrayList<SeamInfo>();
    ArrayList<SeamInfo> seamsWith21 = new ArrayList<SeamInfo>();
    seamsWith21.add(hSeam21);

    t.checkExpect(treeWorldHorizontal.findSentinelAt("left", 1).addBoringHorizSeamTo(seams),
        new ArrayList<SeamInfo>());
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).addBoringHorizSeamTo(seams), seamsWith21);
  }

  void testBrightness(Tester t) {
    this.init();

    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).brightness(), 0.6052287581699346);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).brightness(), 0.526797385620915);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 1).brightness(), 0.4588235294117647);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).brightness(), 0.4326797385620915);
  }

  void testEnergy(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).energy(), 2.2262424227552238);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).energy(), 2.1715971267500453);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2).energy(), 2.311984131089611);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).energy(), 2.505375313942112);
  }

  void testFindPixelAt(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0), vPx00);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 0), vPx20);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1), vPx01);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 1), vPx21);
  }
  
  void testPaintBoringSeamRed(Tester t) {
    this.init();
    t.checkExpect(treeWorldHorizontal.findPixelAt(2,0).redPixel, false);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1,1).redPixel, false);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0,0).redPixel, false);
    treeWorldHorizontal.setBoringSeam(false);
    treeWorldHorizontal.paintBoringSeamRed();
    t.checkExpect(treeWorldHorizontal.findPixelAt(2,0).redPixel, true);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1,1).redPixel, true);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0,0).redPixel, true);
  }

  void testOnKeyEvent(Tester t) {
    this.init();
    
    // test pausing with spacebar
    t.checkExpect(bbTreeWorldHorizontal.automatic, true);
    bbTreeWorldHorizontal.onKeyEvent(" ");
    t.checkExpect(bbTreeWorldHorizontal.automatic, false);
    bbTreeWorldHorizontal.onKeyEvent(" ");
    t.checkExpect(bbTreeWorldHorizontal.automatic, true);
    
    // test energy grayscale with e key
    this.init();
    t.checkExpect(bbTreeWorldHorizontal.energyGrayscale, false);
    bbTreeWorldHorizontal.onKeyEvent("e");
    t.checkExpect(bbTreeWorldHorizontal.energyGrayscale, true);
    bbTreeWorldHorizontal.onKeyEvent("e");
    t.checkExpect(bbTreeWorldHorizontal.energyGrayscale, false);
    
    // test seam grayscale with s key
    this.init();
    t.checkExpect(bbTreeWorldHorizontal.seamGrayscale, false);
    bbTreeWorldHorizontal.onKeyEvent("s");
    t.checkExpect(bbTreeWorldHorizontal.seamGrayscale, true);
    bbTreeWorldHorizontal.onKeyEvent("s");
    t.checkExpect(bbTreeWorldHorizontal.seamGrayscale, false);
    
    // test deleting a vertical seam with v key
    this.init();
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 135);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 101);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 128);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 161);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 114);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 141);

    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).red, 191);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).green, 169);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).blue, 192);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).red, 170);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).green, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).blue, 171);
    
    bbTreeWorldHorizontal.onKeyEvent(" ");
    bbTreeWorldHorizontal.onKeyEvent("v");
    
    t.checkExpect(treeWorldHorizontal.isWellFormed(), true);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 135);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 101);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 128);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 161);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 139);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 122);

    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).red, 191);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).green, 169);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).blue, 192);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).red, 156);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).green, 120);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).blue, 148);
    
 // test deleting a horizontal seam with h key
    this.init();
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 135);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 101);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 128);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 161);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 114);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 141);

    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).red, 127);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).green, 78);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).blue, 110);
  
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).red, 139);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).blue, 122);
    
    bbTreeWorldHorizontal.onKeyEvent(" ");
    bbTreeWorldHorizontal.onKeyEvent("h");
    
    t.checkExpect(treeWorldHorizontal.isWellFormed(), true);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 161);

    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);

    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 191);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 169);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 192);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 170);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 171);

    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).red, 139);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).blue, 122);
  
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).red, 156);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).green, 120);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).blue, 148);
  }
  
  void testOnTick(Tester t) {
    this.init();
    
    // vertical world onTick tests
    t.checkExpect(bbTreeWorldVertical.deletedLastTick, true);
    t.checkExpect(treeWorldVertical.findPixelAt(2,0).red, 127);
    t.checkExpect(treeWorldVertical.findPixelAt(1,1).red, 148);

    bbTreeWorldVertical.onTick(); // paint seam red
    
    t.checkExpect(bbTreeWorldVertical.deletedLastTick, false);
    t.checkExpect(treeWorldVertical.findPixelAt(2,0).redPixel, true);
    t.checkExpect(treeWorldVertical.findPixelAt(1,1).redPixel, true);
    
    bbTreeWorldVertical.onTick(); // delete a seam
    
    t.checkExpect(treeWorldVertical.isWellFormed(), true);
    
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).red, 135);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).green, 101);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).blue, 128);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0).blue, 117);
    
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).red, 168);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).green, 134);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 1).blue, 161);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).red, 139);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).green, 90);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 1).blue, 122);

    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).red, 191);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).green, 169);
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).blue, 192);
    
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).red, 156);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).green, 120);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).blue, 148);
    
    // horizontal world onTick tests
    t.checkExpect(bbTreeWorldHorizontal.deletedLastTick, true);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2,0).red, 127);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1,1).red, 148);
    
    bbTreeWorldHorizontal.onTick(); // paint seam red
    
    t.checkExpect(bbTreeWorldHorizontal.deletedLastTick, false);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2,0).redPixel, true);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1,1).redPixel, true);
    
    bbTreeWorldHorizontal.onTick(); // delete seam
    
    t.checkExpect(treeWorldHorizontal.isWellFormed(), true);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).red, 168);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).green, 134);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).blue, 161);

    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).red, 124);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 0).blue, 117);

    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).red, 191);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).green, 169);
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 1).blue, 192);
    
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).red, 170);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).green, 148);
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 1).blue, 171);

    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).red, 139);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).green, 90);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 0).blue, 122);
  
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).red, 156);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).green, 120);
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 1).blue, 148);
  }
  
  void testShouldWorldEnd(Tester t) {
    this.init();
    
    // no seam deleted, width = 3
    t.checkExpect(bbTreeWorldHorizontal.shouldWorldEnd(), false);
    
    bbTreeWorldHorizontal.onTick();
    bbTreeWorldHorizontal.onTick();
    // one seam deleted, width = 2
    
    bbTreeWorldHorizontal.onTick();
    bbTreeWorldHorizontal.onTick();
    // two seams deleted, width = 1
    
    t.checkExpect(bbTreeWorldHorizontal.shouldWorldEnd(), false);
    
    bbTreeWorldHorizontal.onTick();
    // one seam added
    bbTreeWorldHorizontal.onTick();
    // two seams added
    
    t.checkExpect(bbTreeWorldHorizontal.shouldWorldEnd(), true);
    
    
    this.init();
    
    // no seam deleted, height = 3
    t.checkExpect(bbTreeWorldVertical.shouldWorldEnd(), false);
    
    bbTreeWorldVertical.onTick();
    bbTreeWorldVertical.onTick();
    // one seam deleted, height = 2
    
    bbTreeWorldVertical.onTick();
    bbTreeWorldVertical.onTick();
    // two seams deleted, height = 1
    
    t.checkExpect(bbTreeWorldVertical.shouldWorldEnd(), false);
    
    t.checkExpect(bbTreeWorldVertical.shouldWorldEnd(), false);
    
    bbTreeWorldVertical.onTick();
    // one seam added
    bbTreeWorldVertical.onTick();
    // two seams added
    
    t.checkExpect(bbTreeWorldVertical.shouldWorldEnd(), true);
    
  }
  
  void testCreateCPI(Tester t) {
    this.init();
    
    // test create cpi with normal pixel colors and vertical seams
    ComputedPixelImage cpiColor = treeWorldVertical.createCPI(false, false, true);
    t.checkExpect(cpiColor.getColorAt(0, 0), new Color(135, 101, 128));
    t.checkExpect(cpiColor.getColorAt(1, 0), new Color(124, 90, 117));
    t.checkExpect(cpiColor.getColorAt(2, 0), new Color(127, 78, 110));
    t.checkExpect(cpiColor.getColorAt(0, 1), new Color(168, 134, 161));
    t.checkExpect(cpiColor.getColorAt(1, 1), new Color(148, 114, 141));
    t.checkExpect(cpiColor.getColorAt(2, 1), new Color(139, 90, 122));
    t.checkExpect(cpiColor.getColorAt(0, 2), new Color(191, 169, 192));
    t.checkExpect(cpiColor.getColorAt(1, 2), new Color(170, 148, 171));
    t.checkExpect(cpiColor.getColorAt(2, 2), new Color(156, 120, 148));
    
    // test create cpi with energy grayscale and vertical seams
    ComputedPixelImage cpiEnergy = treeWorldVertical.createCPI(true, false, true);
    t.checkExpect(cpiEnergy.getColorAt(0, 0), new Color(141, 141, 141));
    t.checkExpect(cpiEnergy.getColorAt(1, 0), new Color(136, 136, 136));
    t.checkExpect(cpiEnergy.getColorAt(2, 0), new Color(127, 127, 127));
    t.checkExpect(cpiEnergy.getColorAt(0, 1), new Color(142, 142, 142));
    t.checkExpect(cpiEnergy.getColorAt(1, 1), new Color(61, 61, 61));
    t.checkExpect(cpiEnergy.getColorAt(2, 1), new Color(139, 139, 139));
    t.checkExpect(cpiEnergy.getColorAt(0, 2), new Color(159, 159, 159));
    t.checkExpect(cpiEnergy.getColorAt(1, 2), new Color(138, 138, 138));
    t.checkExpect(cpiEnergy.getColorAt(2, 2), new Color(147, 147, 147));
    
    // test create cpi with vertical seam grayscale
    ComputedPixelImage cpiVertSeam = treeWorldVertical.createCPI(false, true, true);
    t.checkExpect(cpiVertSeam.getColorAt(0, 0), new Color(129, 129, 129));
    t.checkExpect(cpiVertSeam.getColorAt(1, 0), new Color(127, 127, 127));
    t.checkExpect(cpiVertSeam.getColorAt(2, 0), new Color(124, 124, 124));
    t.checkExpect(cpiVertSeam.getColorAt(0, 1), new Color(170, 170, 170));
    t.checkExpect(cpiVertSeam.getColorAt(1, 1), new Color(145, 145, 145));
    t.checkExpect(cpiVertSeam.getColorAt(2, 1), new Color(167, 167, 167));
    t.checkExpect(cpiVertSeam.getColorAt(0, 2), new Color(186, 186, 186));
    t.checkExpect(cpiVertSeam.getColorAt(1, 2), new Color(181, 181, 181));
    t.checkExpect(cpiVertSeam.getColorAt(2, 2), new Color(183, 183, 183));
    
    // test create cpi with horizontal seam grayscale
    ComputedPixelImage cpiHorizSeam = treeWorldHorizontal.createCPI(false, true, false);
    t.checkExpect(cpiHorizSeam.getColorAt(0, 0), new Color(129, 129, 129));
    t.checkExpect(cpiHorizSeam.getColorAt(1, 0), new Color(170, 170, 170));
    t.checkExpect(cpiHorizSeam.getColorAt(2, 0), new Color(182, 182, 182));
    t.checkExpect(cpiHorizSeam.getColorAt(0, 1), new Color(130, 130, 130));
    t.checkExpect(cpiHorizSeam.getColorAt(1, 1), new Color(149, 149, 149));
    t.checkExpect(cpiHorizSeam.getColorAt(2, 1), new Color(184, 184, 184));
    t.checkExpect(cpiHorizSeam.getColorAt(0, 2), new Color(136, 136, 136));
    t.checkExpect(cpiHorizSeam.getColorAt(1, 2), new Color(170, 170, 170));
    t.checkExpect(cpiHorizSeam.getColorAt(2, 2), new Color(186, 186, 186));
    
    // energy and seam grayscale both set to true should throw an exception
    t.checkException(new IllegalArgumentException
        ("A cpi cannot be generated to have both an energy grayscale and a seam grayscale"), 
        treeWorldVertical, "createCPI", true, true, true);
  }
  
  // testing creating all the pixel in the grid
  void testPopulate(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 2), vRight2);
    t.checkExpect(treeWorldVertical.bottomleft, vBottomLeft);
    t.checkExpect(treeWorldVertical.findPixelAt(1, 0), vPx10);
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2), vPx22);
  }
  
  void testCreatePixel(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.createPixel(0, 0).red, 135);
    t.checkExpect(treeWorldVertical.createPixel(0, 0).green, 101);
    t.checkExpect(treeWorldVertical.createPixel(0, 0).blue, 128);
    
    t.checkExpect(treeWorldVertical.createPixel(0, 1).red, 168);
    t.checkExpect(treeWorldVertical.createPixel(0, 1).green, 134);
    t.checkExpect(treeWorldVertical.createPixel(0, 1).blue, 161);
    
    t.checkExpect(treeWorldVertical.createPixel(0, 2).red, 191);
    t.checkExpect(treeWorldVertical.createPixel(0, 2).green, 169);
    t.checkExpect(treeWorldVertical.createPixel(0, 2).blue, 192);
    
    t.checkExpect(treeWorldVertical.createPixel(1, 0).red, 124);
    t.checkExpect(treeWorldVertical.createPixel(1, 0).green, 90);
    t.checkExpect(treeWorldVertical.createPixel(1, 0).blue, 117);
    
    t.checkExpect(treeWorldVertical.createPixel(1, 1).red, 148);
    t.checkExpect(treeWorldVertical.createPixel(1, 1).green, 114);
    t.checkExpect(treeWorldVertical.createPixel(1, 1).blue, 141);
    
    t.checkExpect(treeWorldVertical.createPixel(1, 2).red, 170);
    t.checkExpect(treeWorldVertical.createPixel(1, 2).green, 148);
    t.checkExpect(treeWorldVertical.createPixel(1, 2).blue, 171);
    
    t.checkExpect(treeWorldVertical.createPixel(2, 0).red, 127);
    t.checkExpect(treeWorldVertical.createPixel(2, 0).green, 78);
    t.checkExpect(treeWorldVertical.createPixel(2, 0).blue, 110);
    
    t.checkExpect(treeWorldVertical.createPixel(2, 1).red, 139);
    t.checkExpect(treeWorldVertical.createPixel(2, 1).green, 90);
    t.checkExpect(treeWorldVertical.createPixel(2, 1).blue, 122);
    
    t.checkExpect(treeWorldVertical.createPixel(2, 2).red, 156);
    t.checkExpect(treeWorldVertical.createPixel(2, 2).green, 120);
    t.checkExpect(treeWorldVertical.createPixel(2, 2).blue, 148);
  }
  
  void testCreatePixelColor(Tester t) {
    this.init();
    // world with automatic vertical seam deletion
    
    // normal color of a pixel
    t.checkExpect(treeWorldVertical.createPixelColor
        (treeWorldVertical.findPixelAt(0, 0), false, false, true), new Color(135, 101, 128));
    
    // energy grayscale of a pixel
    t.checkExpect(treeWorldVertical.createPixelColor
        (treeWorldVertical.findPixelAt(0, 0), true, false, true), new Color(141, 141, 141));
    
    // vertical seam grayscale of a pixel
    t.checkExpect(treeWorldVertical.createPixelColor
        (treeWorldVertical.findPixelAt(0, 0), false, true, true), new Color(129, 129, 129));
    
    // energy and seam grayscale both set to true should throw an exception
    t.checkException(new IllegalArgumentException
        ("A color cannot be generated to have both an energy grayscale and a seam grayscale"), 
        treeWorldVertical, "createPixelColor", 
        treeWorldVertical.findPixelAt(0, 0), true, true, true);
    
    // world with automatic horizontal seam deletion
    
    // normal color of a pixel
    t.checkExpect(treeWorldHorizontal.createPixelColor
        (treeWorldHorizontal.findPixelAt(0, 0), false, false, true), new Color(135, 101, 128));
    
    // energy grayscale of a pixel
    t.checkExpect(treeWorldHorizontal.createPixelColor
        (treeWorldHorizontal.findPixelAt(0, 0), true, false, true), new Color(141, 141, 141));
    
    // horizontal seam grayscale of a pixel
    t.checkExpect(treeWorldHorizontal.createPixelColor
        (treeWorldHorizontal.findPixelAt(0, 0), false, true, false), new Color(129, 129, 129));
    
    // energy and seam grayscale both set to true should throw an exception
    t.checkException(new IllegalArgumentException
        ("A color cannot be generated to have both an energy grayscale and a seam grayscale"), 
        treeWorldHorizontal, "createPixelColor", 
        treeWorldHorizontal.findPixelAt(0, 0), true, true, false);
  }
  
  void testCreateFrame(Tester t) {
    this.init();

    // make sure sentinels have correct sentinel neighbors
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 1).getTopLeft(), treeWorldVertical.findSentinelAt("bottom", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 1).left, treeWorldVertical.findSentinelAt("top", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 1).up, treeWorldVertical.findSentinelAt("bottom", 1));
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 1).right, treeWorldVertical.findSentinelAt("top", 2));
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 1).getTopRight(), treeWorldVertical.findSentinelAt("bottom", 2));

    t.checkExpect(treeWorldVertical.findSentinelAt("right", 1).getTopRight(), treeWorldVertical.findSentinelAt("left", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 1).up, treeWorldVertical.findSentinelAt("right", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 1).right, treeWorldVertical.findSentinelAt("left", 1));
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 1).down, treeWorldVertical.findSentinelAt("right", 2));
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 1).getDownRight(), treeWorldVertical.findSentinelAt("left", 2));

    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 1).getDownLeft(), treeWorldVertical.findSentinelAt("top", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 1).left, treeWorldVertical.findSentinelAt("bottom", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 1).down, treeWorldVertical.findSentinelAt("top", 1));
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 1).right, treeWorldVertical.findSentinelAt("bottom", 2));
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 1).getDownRight(), treeWorldVertical.findSentinelAt("top", 2));

    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).getTopLeft(), treeWorldVertical.findSentinelAt("right", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).up, treeWorldVertical.findSentinelAt("left", 0));
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).left, treeWorldVertical.findSentinelAt("right", 1));
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).down, treeWorldVertical.findSentinelAt("left", 2));
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 1).getDownLeft(), treeWorldVertical.findSentinelAt("right", 2));

    // make sure there are 5 sentinels on top
    t.checkExpect(treeWorldVertical.topleft.right.right.right.right, treeWorldVertical.topright);
    t.checkExpect(treeWorldVertical.topright.left.left.left.left, treeWorldVertical.topleft);

    // make sure there are 5 sentinels on the left
    t.checkExpect(treeWorldVertical.topleft.down.down.down.down, treeWorldVertical.bottomleft);
    t.checkExpect(treeWorldVertical.bottomleft.up.up.up.up, treeWorldVertical.topleft);

    // make sure there are 5 sentinels on the right
    t.checkExpect(treeWorldVertical.topright.down.down.down.down, treeWorldVertical.bottomright);
    t.checkExpect(treeWorldVertical.bottomright.up.up.up.up, treeWorldVertical.topright);

    // make sure there are 5 sentinels on the bottom
    t.checkExpect(treeWorldVertical.bottomleft.right.right.right.right, treeWorldVertical.bottomright);
    t.checkExpect(treeWorldVertical.bottomright.left.left.left.left, treeWorldVertical.bottomleft);
  }
  
  void testSetRight(Tester t) {
    this.init();
    pxRed.setRight(pxGreen);
    t.checkExpect(pxRed.right, pxGreen);
    t.checkExpect(pxGreen.left, pxRed);
  }

  void testSetLeft(Tester t) {
    this.init();
    pxRed.setLeft(pxGreen);
    t.checkExpect(pxRed.left, pxGreen);
    t.checkExpect(pxGreen.right, pxRed);
  }

  void testSetUp(Tester t) {
    this.init();
    pxRed.setUp(pxGreen);
    t.checkExpect(pxRed.up, pxGreen);
    t.checkExpect(pxGreen.down, pxRed);
  }

  void testSetDown(Tester t) {
    this.init();
    pxRed.setDown(pxGreen);
    t.checkExpect(pxRed.down, pxGreen);
    t.checkExpect(pxGreen.up, pxRed);
  }

  void testGetTopLeft(Tester t) {
    this.init();
    t.checkExpect(vPx11.getTopLeft(), vPx00);
    t.checkExpect(vPx00.getTopLeft(), vTopLeft);
  }

  void testGetTopRight(Tester t) {
    this.init();
    t.checkExpect(vPx11.getTopRight(), vPx20);
    t.checkExpect(vPx20.getTopRight(), vTopRight);
  }

  void testGetDownLeft(Tester t) {
    this.init();
    t.checkExpect(vTopRight.getDownLeft(), vPx20);
    t.checkExpect(vPx01.getDownLeft(), vLeft3);
  }

  void testGetDownRight(Tester t) {
    this.init();
    t.checkExpect(vTop1.getDownRight(), vPx10);
    t.checkExpect(vPx10.getDownRight(), vPx21);
  }

  void testFindSentinelAt(Tester t) {
    this.init();
    treeWorldVertical.createSentinelFrame();

    t.checkExpect(treeWorldVertical.findSentinelAt("top", 0), treeWorldVertical.topleft);
    t.checkExpect(treeWorldVertical.findSentinelAt("top", 4), treeWorldVertical.topright);
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 0), treeWorldVertical.topright);
    t.checkExpect(treeWorldVertical.findSentinelAt("right", 4), treeWorldVertical.bottomright);
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 0), treeWorldVertical.bottomleft);
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 2).left.left, treeWorldVertical.bottomleft);
    t.checkExpect(treeWorldVertical.findSentinelAt("bottom", 4), treeWorldVertical.bottomright);
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 0), treeWorldVertical.topleft);
    t.checkExpect(treeWorldVertical.findSentinelAt("left", 4), treeWorldVertical.bottomleft);
  }
  
  void testGetEnergyGrayscale(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).getEnergyGrayscale(), new Color(141, 141, 141));
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).getEnergyGrayscale(), new Color(138, 138, 138));
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2).getEnergyGrayscale(), new Color(147, 147, 147));
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).getEnergyGrayscale(), new Color(159, 159, 159));
  }
  
  void testGetSeamGrayscale(Tester t) {
    this.init();
    t.checkExpect(treeWorldVertical.findPixelAt(0, 0).getSeamGrayscale(3, true), new Color(129, 129, 129));
    t.checkExpect(treeWorldVertical.findPixelAt(1, 2).getSeamGrayscale(3, true), new Color(181, 181, 181));
    t.checkExpect(treeWorldVertical.findPixelAt(2, 2).getSeamGrayscale(3, true), new Color(183, 183, 183));
    t.checkExpect(treeWorldVertical.findPixelAt(0, 2).getSeamGrayscale(3, true), new Color(186, 186, 186));
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 0).getSeamGrayscale(3, false), new Color(129, 129, 129));
    t.checkExpect(treeWorldHorizontal.findPixelAt(1, 2).getSeamGrayscale(3, false), new Color(170, 170, 170));
    t.checkExpect(treeWorldHorizontal.findPixelAt(2, 2).getSeamGrayscale(3, false), new Color(186, 186, 186));
    t.checkExpect(treeWorldHorizontal.findPixelAt(0, 2).getSeamGrayscale(3, false), new Color(136, 136, 136));
  }

  // Big Bang: tests deleting seams
  void testBigBang(Tester t) {
    this.initAllTestWorlds();
    
    int worldWidth = 500;
    int worldHeight = 500;
    double tickRate = .05;
    bbBalloonsWorldHorizontal.bigBang(worldWidth, worldHeight, tickRate);
  }

}