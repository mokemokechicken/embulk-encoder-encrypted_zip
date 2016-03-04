package org.embulk.encoder.encrypted_zip;

import java.io.OutputStream;
import java.io.IOException;
import com.google.common.base.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigInject;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.EncoderPlugin;
import org.embulk.spi.FileOutput;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.util.FileOutputOutputStream;
import org.embulk.spi.util.OutputStreamFileOutput;

public class EncryptedZipEncoderPlugin
        implements EncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        // configuration option 1 (required integer)
        @Config("option1")
        public int getOption1();

        // configuration option 2 (optional string, null is not allowed)
        @Config("optoin2")
        @ConfigDefault("\"myvalue\"")
        public String getOption2();

        // configuration option 3 (optional string, null is allowed)
        @Config("optoin3")
        @ConfigDefault("null")
        public Optional<String> getOption3();

        @ConfigInject
        public BufferAllocator getBufferAllocator();
    }

    @Override
    public void transaction(ConfigSource config, EncoderPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        control.run(task.dump());
    }

    @Override
    public FileOutput open(TaskSource taskSource, FileOutput fileOutput)
    {
        final PluginTask task = taskSource.loadTask(PluginTask.class);

        // If expect OutputStream, you can use this code:

        final FileOutputOutputStream output = new FileOutputOutputStream(fileOutput,
            task.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH);

        return new OutputStreamFileOutput(new OutputStreamFileOutput.Provider() {
            public OutputStream openNext() throws IOException
            {
                output.nextFile();
                return newEncoderOutputStream(task, output);
            }

            public void finish() throws IOException
            {
                output.finish();
            }

            public void close() throws IOException
            {
                output.close();
            }
        });
    }

    private static OutputStream newEncoderOutputStream(PluginTask task, OutputStream file) throws IOException
    {
        return file;
    }
}
