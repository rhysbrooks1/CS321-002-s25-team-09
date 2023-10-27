package cs321.create;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SSHCreateBTreeTest
{
    private String[] args;
    private SSHCreateBTreeArguments expectedConfiguration;
    private SSHCreateBTreeArguments actualConfiguration;

    @Test
    public void parse4CorrectArgumentsTest()
    {
        args = new String[4];
        args[0] = "0";
        args[1] = "20";
        args[2] = "fileNameGbk.gbk";
        args[3] = "13";

        expectedConfiguration = new SSHCreateBTreeArguments(false, 20, "fileNameGbk.gbk", "13", 0, 0);
//        actualConfiguration = SSHCreateBTree.parseArguments(args);
//        assertEquals(expectedConfiguration, actualConfiguration);
    }

}
