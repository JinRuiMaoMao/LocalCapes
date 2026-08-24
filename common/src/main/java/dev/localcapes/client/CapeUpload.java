package dev.localcapes.client;

import dev.localcapes.LocalCapes;
import org.jetbrains.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Frame;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class CapeUpload {
    public enum Status {
        CANCELLED,
        FAILED,
        SUCCESS
    }

    public record Result(Status status, @Nullable Path path) {
        public static Result cancelled() {
            return new Result(Status.CANCELLED, null);
        }

        public static Result failed() {
            return new Result(Status.FAILED, null);
        }

        public static Result success(Path path) {
            return new Result(Status.SUCCESS, path);
        }
    }

    private CapeUpload() {
    }

    public static Result pickPng() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            Result windows = pickPngWindows();
            if (windows.status() != Status.FAILED) {
                return windows;
            }
        } else if (os.contains("mac")) {
            Result mac = pickPngMac();
            if (mac.status() != Status.FAILED) {
                return mac;
            }
        } else {
            Result zenity = pickPngZenity();
            if (zenity.status() != Status.FAILED) {
                return zenity;
            }
        }
        return pickPngSwing();
    }

    private static Result pickPngWindows() {
        String command = """
                Add-Type -AssemblyName System.Windows.Forms;
                $dialog = New-Object System.Windows.Forms.OpenFileDialog;
                $dialog.Filter = 'PNG files (*.png)|*.png';
                $dialog.Title = 'LocalCapes';
                if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
                    Write-Output $dialog.FileName
                }
                """;
        try {
            Process process = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-STA", "-WindowStyle", "Hidden", "-Command", command
            ).redirectErrorStream(true).start();
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                return Result.failed();
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (output.isEmpty()) {
                return Result.cancelled();
            }
            if (process.exitValue() != 0) {
                LocalCapes.LOGGER.error("Windows file picker failed: {}", output);
                return Result.failed();
            }
            Path path = Path.of(output);
            return Files.isRegularFile(path) ? Result.success(path) : Result.failed();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LocalCapes.LOGGER.error("Windows file picker failed", e);
            return Result.failed();
        }
    }

    private static Result pickPngMac() {
        String[] command = {
                "osascript", "-e",
                "POSIX path of (choose file with prompt \"LocalCapes\" of type {\"png\"})"
        };
        return runProcessPicker(command);
    }

    private static Result pickPngZenity() {
        String[] command = {
                "zenity", "--file-selection", "--title=LocalCapes", "--file-filter=*.png"
        };
        return runProcessPicker(command);
    }

    private static Result runProcessPicker(String[] command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                return Result.failed();
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (output.isEmpty() || process.exitValue() != 0) {
                return Result.cancelled();
            }
            Path path = Path.of(output);
            return Files.isRegularFile(path) ? Result.success(path) : Result.failed();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LocalCapes.LOGGER.error("Native file picker failed", e);
            return Result.failed();
        }
    }

    private static Result pickPngSwing() {
        System.setProperty("java.awt.headless", "false");
        final Path[] picked = new Path[1];
        final boolean[] failed = {false};
        try {
            SwingUtilities.invokeAndWait(() -> {
                Frame owner = new Frame();
                owner.setAlwaysOnTop(true);
                owner.setVisible(true);
                owner.toFront();
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("LocalCapes");
                    chooser.setFileFilter(new FileNameExtensionFilter("PNG cape (*.png)", "png"));
                    chooser.setAcceptAllFileFilterUsed(false);
                    if (chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                        picked[0] = chooser.getSelectedFile().toPath();
                    }
                } catch (Exception e) {
                    failed[0] = true;
                    LocalCapes.LOGGER.error("Swing file picker failed", e);
                } finally {
                    owner.dispose();
                }
            });
        } catch (Exception e) {
            LocalCapes.LOGGER.error("Could not open Swing file picker", e);
            return Result.failed();
        }
        if (failed[0]) {
            return Result.failed();
        }
        Path path = picked[0];
        if (path == null) {
            return Result.cancelled();
        }
        return Files.isRegularFile(path) ? Result.success(path) : Result.failed();
    }
}
