package handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FileStorageHandler extends SimpleChannelInboundHandler<String> {
    private static final String LS_COMMAND         = "\tls          view all files from current directory\r\n";
    private static final String MKDIR_COMMAND      = "\tmkdir       create directory, format : mkdir new_directory\r\n";
    private static final String TOUCH_COMMAND      = "\ttouch       create file, format : touch new_file\r\n";
    private static final String CD_COMMAND         = "\tcd          change directory, format : cd ~ | .. | new_path \r\n";
    private static final String RM_COMMAND         = "\trm          remove file / directory, format : rm file | directory\r\n";
    private static final String COPY_COMMAND       = "\tcopy        copy file / directory, format : copy source destination\r\n";
    private static final String CAT_COMMAND        = "\tcat         get textfile context, format : cat file\r\n";
    private static final String CHANGENICK_COMMAND = "\tchangenick  change user nick, format : changenick new_nick\r\n";

    //список поддерживаемых команд
    private final List<String> commands = Arrays.asList(new String[]{"--help", "changenick", "ls", "mkdir", "touch", "cd", "rm", "copy", "cat", "Y", "N"});
    private static String lastCommandWithResponseRequest;
    private static String lastCommandParam;

    private String clientNick;
    private ServerLogic serverLogic;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client connected: " + ctx.channel());
        clientNick = ctx.name();
        serverLogic = new ServerLogic(Path.of("server"));  //Пока все пользователи работают с одним каталогом на сервере
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client disconnected: " + ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {

        String command = s.replace("\n", "").replace("\r", ""); //получаем команду
        String[] cmd = command.split(" ");

        List<String> response = handleCommand(command);
        if (response.size() > 0) {
            for (String msg : response) {
                ctx.writeAndFlush(msg);
            }
        }
        ctx.writeAndFlush(getUserWelcome()); //приглашение пользователю для ввода команды
    }

    /**
     * Функция проверяет, что пользователь ввёл правильное количество параметров для команды
     * */
    private boolean checkCommandParams(String[] cmd) {
        if ("ls".equals(cmd[0]) || "--help".equals(cmd[0]) || "Y".equals(cmd[0]) || "N".equals(cmd[0])) { //команда без параметров
            return cmd.length == 1;

        } else if ("copy".equals(cmd[0])) { //в команде должно быть указано два параметра
            return cmd.length == 3;

        } else  { //все остальные команды с одним параметром
            return cmd.length == 2;
        }
    }

    private String getUserWelcome() {
        return "\r\n" + clientNick + " " + serverLogic.getUserPath() + " : ";
    }

    /**
     * Обработчик команд
     * */
    private List<String> handleCommand(String command) {
        String[] cmd = command.split(" ");

        List<String> result = new ArrayList<>();

        if (commands.contains(cmd[0])) { //проверяем, что введена известная серверу команда
            if (!checkCommandParams(cmd)) { //проверяем, что параметры команды указаны корректно
                result.add("unknown parameters\r\n"); //Посылаем сообщение - неправильно указаны параметры
            } else {
                try {
                    switch (cmd[0]) {
                        case "--help":
                            result.add(LS_COMMAND);
                            result.add(MKDIR_COMMAND);
                            result.add(TOUCH_COMMAND);
                            result.add(CD_COMMAND);
                            result.add(RM_COMMAND);
                            result.add(COPY_COMMAND);
                            result.add(CAT_COMMAND);
                            result.add(CHANGENICK_COMMAND);
                            break;

                        case "changenick":
                            clientNick = cmd[1];
                            result.add("user nick has been changed\r\n");
                            break;

                        case "ls":
                            result.add(serverLogic.getFilesList());
                            break;

                        case "mkdir":
                            result.add(serverLogic.createDirectory(cmd[1]));
                            break;

                        case "touch":
                            result.add(serverLogic.createFile(cmd[1]));
                            break;

                        case "cd":
                            serverLogic.changeDirectory(cmd[1]);
                            break;

                        case "rm":
                            lastCommandWithResponseRequest = RM_COMMAND; //Последняя команда и её параметры
                            lastCommandParam = cmd[1];                   //Сохраняем, так как будет вопрос пользователю на
                            //удаление непустого каталога
                            result.add(serverLogic.removeFileOrDirectory(cmd[1]));
                            break;

                        case "N": //Ответ пользователя "N", всё обнуляем, ничего не делаем
                            if (lastCommandWithResponseRequest != null) {
                                lastCommandWithResponseRequest = null;
                                lastCommandParam = null;
                            }
                            break;

                        case "Y": //Ответ пользователя "Y". Пока обрабатываем только удаление непустого каталога
                            if (lastCommandWithResponseRequest != null && lastCommandWithResponseRequest.equals(RM_COMMAND)) {
                                serverLogic.deleteNotEmptyDirectory(lastCommandParam);
                                lastCommandWithResponseRequest = null;
                                lastCommandParam = null;
                                result.add("deleted\r\n");
                            }
                            break;

                        case "copy":
                            serverLogic.copy(cmd[1], cmd[2]);
                            result.add("copied\r\n");
                            break;

                        case "cat":
                            result.addAll(serverLogic.viewTextFile(cmd[1]));
                            result.add("\r\n");
                            break;

                    }

                } catch (Exception e) {  //Могут быть исключения FileAlreadyExistsException, NoSuchFileException,
                    // IllegalArgumentException
                    //но обработка одна - отправляем пользователю сообщение
                    result.add(e.getMessage());
                }
            }
        } else {
            result.add("unknown command\r\n"); //Посылаем сообщение - неизвестная команда
        }

        return result;
    }

}
