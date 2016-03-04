package org.embulk.encoder.encrypted_zip;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Optional;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.util.Zip4jConstants;
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
import net.lingala.zip4j.model.ZipParameters;


public class EncryptedZipEncoderPlugin
        implements EncoderPlugin
{
    public interface PluginTask
            extends Task
    {
        // configuration option 1 (required integer)
//        @Config("option1")
//        public int getOption1();

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
        return new OutputStreamFileOutput(new ZipCompressArchiveProvider(task, fileOutput));
    }
}

class ZipCompressArchiveProvider implements OutputStreamFileOutput.Provider {
    private final FileOutputOutputStream output;
    private final FileOutput originalOutput;
    private ByteArrayOutputStream tmpOut;
    private ZipOutputStream zipOutputStream;
    private final ZipParameters parameters;
    //
    private static final AtomicInteger baseNumSeq = new AtomicInteger();
    private final int baseNum;
    private final String entryNamePrefix;
    private int count = 0;

    ZipCompressArchiveProvider(EncryptedZipEncoderPlugin.PluginTask task, FileOutput fileOutput) {
        this.originalOutput = fileOutput;
        this.output = new FileOutputOutputStream(fileOutput,
                task.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH);
        this.baseNum = baseNumSeq.getAndIncrement();
        this.entryNamePrefix = "prefix";

        this.parameters = new ZipParameters();
        parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
        parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
        parameters.setPassword("pass");
    }

    @Override
    public OutputStream openNext() throws IOException {
        output.nextFile();
        return tmpOut = new ByteArrayOutputStream();
    }

    @Override
    public void finish() throws IOException {
        if (tmpOut != null) {
            this.zipOutputStream = new ZipOutputStream(output);
            final String name =  String.format(entryNamePrefix, baseNum, count++);
            File file = new File(name) {
                @Override
                public boolean exists() {return true;}
                @Override
                public boolean isDirectory() {return false;}
                @Override
                public String getAbsolutePath() {return name;}
                @Override
                public boolean isHidden() {return false;}
                @Override
                public long lastModified() {return System.currentTimeMillis();}
                @Override
                public long length() {return tmpOut.size();}
            };

            try {
                zipOutputStream.putNextEntry(file, parameters);
                zipOutputStream.write(tmpOut.toByteArray());
                zipOutputStream.closeEntry();
                zipOutputStream.finish();
            } catch (ZipException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        finish();
        if (zipOutputStream != null) {
            zipOutputStream.close();
            zipOutputStream = null;
        }

        if (originalOutput != null) {
            originalOutput.close();
        }
    }
}