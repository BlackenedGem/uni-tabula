import static org.junit.Assert.*;
import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by rfkd33 on 13/04/2017.
 */
public class GameCLITest {
    private Colour col0;
    private Colour col1;
    private GameInterface g;
    public GameCLITest() {
        col0 = Colour.values()[0];
        col1 = Colour.values()[1];
    }

    @Before
    public void setUp() {
        g = new GameCLI();
    }

    @After
    public void tearDown() {

    }

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();
    /*Main menu tests
    * 1) Play a) Mode Set b) No Mode Set
    * 2) Mode a) H H b) H C c) C H
    * 3) Load
    * 4) Quit
    * Test should only test the next out is from the next method called.
    * */

    private void caseModeFramework(String in, Class p1, Class p2) throws Exception{
        systemInMock.provideLines(in);
        Scanner scanner = new Scanner(System.in);
        Method caseMode = GameCLI.class.getDeclaredMethod("caseMode", Scanner.class);
        caseMode.setAccessible(true);
        caseMode.invoke(g,scanner);

        Field players = GameCLI.class.getSuperclass().getDeclaredField("players");
        players.setAccessible(true);
        Map<Colour, PlayerInterface> p = (Map)players.get(g);
        if (p1 != null && p2 != null) {
            assertEquals(p.get(col0).getClass(), p1);
            assertEquals(p.get(col1).getClass(), p2);
            assertFalse(systemOutRule.getLog().contains("Not a valid player mode"));
        } else {
            assertTrue(systemOutRule.getLog().contains("Not a valid player mode"));
        }
    }

    @Test
    public void caseMode1() throws Exception{
        caseModeFramework("1", PlayerCLI.class, PlayerCLI.class);
    }

    @Test
    public void caseMode2() throws Exception{
        caseModeFramework("2", PlayerCLI.class, PlayerComputerOld.class);
    }

    @Test
    public void caseMode3() throws Exception{
        caseModeFramework("3", PlayerComputerOld.class, PlayerCLI.class);
    }

    @Test
    public void caseMode4() throws Exception{
        caseModeFramework("4", PlayerComputerOld.class, PlayerComputerOld.class);
    }

    @Test
    public void caseMode5() throws Exception{
        caseModeFramework("5", null, null);
    }

    private void caseLoadFramework(String fileName, boolean load) throws Exception{
        systemInMock.provideLines(fileName);
        Scanner scanner = new Scanner(System.in);
        Method caseMode = GameCLI.class.getDeclaredMethod("caseLoad", Scanner.class);
        caseMode.setAccessible(true);
        caseMode.invoke(g,scanner);

        if (load) {
            Field board = GameCLI.class.getSuperclass().getDeclaredField("board");
            board.setAccessible(true);
            BoardInterface b1 = (Board) board.get(g);
            GameCLI g2 = new GameCLI();
            g2.loadGame(fileName);
            BoardInterface b2 = (Board) board.get(g2);
            assertEquals(b2.toString(), b1.toString());
        } else {
            assertTrue(systemOutRule.getLog().contains("Not a valid save file"));
        }
    }

    @Test
    public void caseLoad1() throws Exception{
        caseLoadFramework("testLoad.json", true);
    }

    @Test
    public void caseLoad2() throws Exception{
        caseLoadFramework("testLoad2.json", true);
    }

    @Test
    public void caseLoad3() throws Exception{
        caseLoadFramework("willNotLoadFile", false);
    }

    @Test
    public void sequentialLoad() throws Exception{
        exit.expectSystemExitWithStatus(0);
        try {
            caseLoadFramework("testLoad.json", true);
            systemInMock.provideLines("p", "m", "y", "q");
            Method play = g.getClass().getDeclaredMethod("play");
            play.invoke(g);
            caseLoadFramework("testLoad2.json", true);
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
               throw new AssertionError();
            }
        }
    }

    private void caseSaveFramework(String fileName, boolean delete) throws Exception{
        //systemInMock.provideLines(fileName);
        Scanner scanner = new Scanner(System.in);
        g.setPlayer(col0,new PlayerCLI());
        g.setPlayer(col1,new PlayerCLI());
        Field dice = g.getClass().getSuperclass().getDeclaredField("dice");
        dice.setAccessible(true);
        Dice d = (Dice)dice.get(g);
        d.roll();

        Method saveCase = GameCLI.class.getDeclaredMethod("caseSave", Scanner.class);

        saveCase.setAccessible(true);
        saveCase.invoke(g, scanner);

        File gameSaves = new File("gameSaves");
        File[] listOfSaves = gameSaves.listFiles();
        List<String> fnames = new ArrayList<>();
        for(File  f: listOfSaves){
            fnames.add(f.getName());
        }


        assertTrue(fnames.contains(fileName));
        if (delete) {
            File f = new File("gameSaves/" + fileName);
            f.delete();
        }

    }

    /*Save direct
    * Save overwrite no
    * Save overwrite yes
    * Save overwrite not an option
    * No file name
    */
    //No input doesnt do anything - no need to test
    @Test
    public void saveDirect() throws Exception{
        systemInMock.provideLines("abcSave");
        caseSaveFramework("abcSave.json", true);
    }

    @Test
    public void overwriteYes() throws Exception{
        systemInMock.provideLines("owsave");
        caseSaveFramework("owsave.json", false);
        systemInMock.provideLines("owsave","y");
        caseSaveFramework("owsave.json",true);
    }

    @Test
    public void overwriteNo() throws Exception{
        systemInMock.provideLines("testLoad", "n");
        caseSaveFramework("testLoad.json", false);
        caseLoadFramework("testLoad.json", true);
        assertEquals(g.getCurrentPlayer(), col0);
    }

    @Test
    public void overwriteNotOption() throws Exception{
        systemInMock.provideLines("testLoad", "?");
        caseSaveFramework("testLoad.json", false);
        assertTrue(systemOutRule.getLog().contains("Not an option. Not saving."));
        caseLoadFramework("testLoad.json", true);
        assertEquals(g.getCurrentPlayer(), col0);
    }

    /*caseMainMenu
    * y
    * n
    * not an option
    * */

    private void caseMainMenuFramework(String checkLog) throws Exception{
        Scanner scanner = new Scanner(System.in);
        Method menu = g.getClass().getDeclaredMethod("caseMainMenu", Scanner.class);
        menu.setAccessible(true);
        menu.invoke(g, scanner);
        assertTrue(systemOutRule.getLog().contains(checkLog));
    }

    @Test
    public void caseMainMenuNo() throws Exception{
        systemInMock.provideLines("n");
        caseMainMenuFramework("Resuming game");
    }

    @Test
    public void caseMainMenuInvalidOption() throws Exception{
        systemInMock.provideLines("?");
        caseMainMenuFramework("Not an option, not quitting");
    }

    @Test//will terminate because function doesnt return
    public void caseMainMenuYes() throws Exception{
        exit.expectSystemExitWithStatus(1);
        try {
            systemInMock.provideLines("y");
            caseMainMenuFramework("What would you like to do? [P]lay, [M]ode, [L]oad game, [Q]uit");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    /*pauseMenu
    * r
    * s
    * m
    * invalid option
    * */

    private void casePauseMenuFramework(String checkLog) throws Exception{
        //Scanner scanner = new Scanner(System.in);
        Method pauseMenu = g.getClass().getDeclaredMethod("pauseMenu");
        pauseMenu.setAccessible(true);
        pauseMenu.invoke(g);
        assertTrue(systemOutRule.getLog().contains(checkLog));
    }

    @Test
    public void pauseResume() throws Exception{
        systemInMock.provideLines("r");
        casePauseMenuFramework("Resuming game");
    }

    @Test
    public void pauseMainMenu() throws Exception{
        systemInMock.provideLines("m","n");
        casePauseMenuFramework("What would you like to do? [R]esume, [S]ave, [M]ain menu");
        String log = systemOutRule.getLog();
        String[] lines = log.split("\\r?\\n");
        assertEquals(lines.length, 3);
        assertEquals(lines[0], "What would you like to do? [R]esume, [S]ave, [M]ain menu");
        assertEquals(lines[1], "Any unsaved progress may be lost. Are you sure you want to quit? Y/N: ");
        assertEquals(lines[2], "Resuming game");
    }

    @Test
    public void pauseSave() throws Exception{
        systemInMock.provideLines("s","testLoad","n");
        casePauseMenuFramework("Name of save: ");
    }

    @Test
    public void pauseInvalidOption() throws Exception{
        systemInMock.provideLines("a","a","a","a","r");
        casePauseMenuFramework("Resuming game");
        String log = systemOutRule.getLog();
        String[] lines = log.split("\\r?\\n");
        for (int i=0; i<4;i++){
            assertEquals(lines[i*2], "What would you like to do? [R]esume, [S]ave, [M]ain menu");
            assertEquals(lines[(i*2)+1],"Not an option");
        }
        assertEquals(lines[8], "What would you like to do? [R]esume, [S]ave, [M]ain menu");
        assertEquals(lines[9], "Resuming game");
        assertEquals(lines.length, 10);
    }

    /* mainMenu
    * l
    * p
    * m
    * q
    * invalid option
    * */
    private void mainMenuFramework(String checkLog) throws Exception{
        Method mainMenu = g.getClass().getDeclaredMethod("mainMenu");
        mainMenu.setAccessible(true);
        mainMenu.invoke(g);
        String log = systemOutRule.getLog();
        String[] lines = log.split("\\r?\\n");
        String last = lines[lines.length-1];
        assertEquals(last,checkLog);
    }

    @Test
    public void mainMenuLoad() throws Exception{
        exit.expectSystemExitWithStatus(1);
        try {
            systemInMock.provideLines("l");
            mainMenuFramework("What is the name of the save you would like to load (don't forget to add the file extension)? ");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    @Test
    public void mainMenuPlay() throws Exception{
        exit.expectSystemExitWithStatus(1);
        try {
            systemInMock.provideLines("p");
            mainMenuFramework("Game mode not defined yet");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    @Test
    public void mainMenuMode() throws Exception{
        exit.expectSystemExitWithStatus(1);
        try {
            systemInMock.provideLines("m");
            mainMenuFramework("Select player mode: [1]Human vs. Human, [2]Human vs. Computer, [3]Computer vs. Human, [4]Computer vs. Computer");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    @Test
    public void mainMenuInvalidOption() throws Exception{
        exit.expectSystemExitWithStatus(1);
        try{
            //use m to generate no such element exception instead of sys exit
            systemInMock.provideLines("?","m");
            mainMenuFramework("What would you like to do? [P]lay, [M]ode, [L]oad game, [Q]uit");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    @Test
    public void mainMenuQuit() throws Exception{
        exit.expectSystemExitWithStatus(0);
        try {
            systemInMock.provideLines("q");
            mainMenuFramework("");
        } catch (InvocationTargetException e){
            if (!e.getCause().getClass().equals(CheckExitCalled.class)){
                throw new AssertionError();
            }
        }
    }

    @Test(expected = PlayerNotDefinedException.class)
    public void play() throws Exception{
        try {
            Method p = g.getClass().getDeclaredMethod("play");
            p.setAccessible(true);
            p.invoke(g);
        } catch (InvocationTargetException e){
            if (e.getCause().getClass().equals(PlayerNotDefinedException.class)){
                throw new PlayerNotDefinedException("Player not found");
            } else {
                throw new AssertionError();
            }
        }
    }

}
