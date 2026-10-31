package com.xtc.utils.system;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * 命令执行工具。原厂类名为单字母 {@code m}，内部结果类为 {@code m.a}，
 * 这里还原为 {@code ShellUtils} / {@link ExecResult}。
 *
 * <p>实现要点（与原件一致）：</p>
 * <ul>
 *   <li>用一个进程把多条命令依次写进去，最后写 {@code exit} 再 waitFor；</li>
 *   <li>{@code root=true} 时用 {@code su}，否则用 {@code sh}；</li>
 *   <li>第三条命令的 stdout / stderr 会被拼成一整行（去掉换行），原样返回；</li>
 *   <li>任何异常都只打堆栈，返回码置 -1，不向上抛。</li>
 * </ul>
 */
public class ShellUtils {

    /** 命令执行结果。字段名沿用原件的 a/b/c，改成了有语义的名字。 */
    public static class ExecResult {
        /** 进程退出码，异常时为 -1。 */
        public int code;
        /** 标准输出（已去掉换行）。 */
        public String stdout;
        /** 标准错误（已去掉换行）。 */
        public String stderr;

        public ExecResult(int code, String stdout, String stderr) {
            this.code = code;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    private ShellUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    public static ExecResult exec(String command, boolean root) {
        return exec(new String[]{command}, root, true);
    }

    public static ExecResult exec(String command, boolean root, boolean wantOutput) {
        return exec(new String[]{command}, root, wantOutput);
    }

    public static ExecResult exec(List<String> commands, boolean root) {
        return exec(commands == null ? null : commands.toArray(new String[0]), root, true);
    }

    public static ExecResult exec(List<String> commands, boolean root, boolean wantOutput) {
        return exec(commands == null ? null : commands.toArray(new String[0]), root, wantOutput);
    }

    public static ExecResult exec(String[] commands, boolean root) {
        return exec(commands, root, true);
    }

    /**
     * 执行一组命令。
     */
    public static ExecResult exec(String[] commands, boolean root, boolean wantOutput) {
        if (commands == null || commands.length == 0) {
            return new ExecResult(-1, null, null);
        }
        Process process = null;
        DataOutputStream stdin = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;
        StringBuilder stdout = null;
        StringBuilder stderr = null;
        int exitCode = -1;
        try {
            process = Runtime.getRuntime().exec(root ? "su" : "sh");
            stdin = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command != null) {
                    stdin.write(command.getBytes());
                    stdin.writeBytes("\n");
                    stdin.flush();
                }
            }
            stdin.writeBytes("exit\n");
            stdin.flush();
            exitCode = process.waitFor();
            if (wantOutput) {
                stdout = new StringBuilder();
                stderr = new StringBuilder();
                stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdout.append(line);
                }
                while ((line = stderrReader.readLine()) != null) {
                    stderr.append(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeSilently(stdin, stdoutReader, stderrReader);
            if (process != null) {
                process.destroy();
            }
        }
        return new ExecResult(exitCode,
                stdout == null ? null : stdout.toString(),
                stderr == null ? null : stderr.toString());
    }
}
