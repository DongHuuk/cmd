package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class ExampleController {

    private String cd = System.getProperty("user.dir");

    // window => %3A = :, %5C = \, %2F = /
    private String cmdReplaceList(List list) {
        return list.toString().replace("[", "").replace("]", "").replace(",", " ")
                .replace("%3A", ":").replace("%5C", "\\").replace("%2F", "/").replace("  ", " ");
    }

    private void windowCmdLine(ModelMap modelMap, String args, String[] cmd) throws IOException, InterruptedException {
        //공백 구분 (window에서는 input으로 입력 받을시 공백 값이 변경됨)
        String[] split = args.split("%20");
        String str = "";
        for (String s : split) {
            str = str + " " + s;
        }
        cmd = new String[]{"cmd.exe", "/y /c", str};

        Process process = Runtime.getRuntime().exec(cmd);
        StringBuffer output = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "EUC-KR"));
        String line;

        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.replace("<DIR>", "[DIR]");
            output.append(line + "</br>");
        }

        modelMap.put("line", output);
        process.waitFor();
        reader.close();
    }

    private boolean linuxCmdLine(ModelMap modelMap, String[] cmd) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(cmd);
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while (true) {
            line = reader.readLine();
            if(line == null) break;
            buffer.append(line + "<br/>");
        }
        process.waitFor();

        if(!buffer.equals("") || buffer != null){
            modelMap.put("line", buffer);
            return true;
        }
        return false;
    }

    private String linuxCmdRunPWD(String[] cmd, String cd) throws IOException, InterruptedException {
        cmd[2] = "cd " + cd + " ; pwd";
        Process exec = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        String pathValue = ""; //pwd 결과값 저장
        String str; //임시 저장소

        //결과값
        while (true) {
            str = reader.readLine();
            if(str == null) break;
            pathValue = str;
        }
        exec.waitFor();
        //요청은 cd .. 을 받았으므로 현재위치 기반으로 ..을 동작시킴
        reader.close();
        return pathValue + "/..";
    }

    @RequestMapping("/test.do")
    public String getCmdLine(ModelMap modelMap, @RequestParam(required = false) String args, @RequestParam(required = false) Boolean reset) {

        String osName = System.getProperty("os.name");
        String[] cmd = new String[3];

        if (!osName.toLowerCase().startsWith("window") && (reset != null && reset)) {
            this.cd = System.getProperty("user.dir");
        }

        //os 구분
        if(osName.toLowerCase().startsWith("window")){
            if (args != null && !args.equals("")) {
                try{
                    //window cmd line 동작 method
                    this.windowCmdLine(modelMap, args, cmd);
                }catch (IOException | InterruptedException e) {
                    modelMap.put("error", "명령어가 잘못되었습니다.");
                }
            }
        }else {
            //linux
            cmd = new String[]{"/bin/sh", "-c", args + ";"};

            if (args != null && !args.equals("")) {
                try{
                    this.linuxCmdLine(modelMap, cmd);
                } catch (InterruptedException | IOException e) {
                    modelMap.put("error", "잘못된 명령입니다.");
                }
            }
        }

        return "test";
    }


//    @RequestMapping("/test.do")
//    public String getCmdLine(ModelMap modelMap, @RequestParam(required = false) String args, @RequestParam(required = false) Boolean reset) throws UnsupportedEncodingException {
//        String osName = System.getProperty("os.name");
//        String[] cmd = new String[3];
//
//        if (!osName.toLowerCase().startsWith("window") && (reset != null && reset)) {
//            this.cd = System.getProperty("user.dir");
//        }
//
//        //os 구분
//        if(osName.toLowerCase().startsWith("window")){
//            if (args != null && !args.equals("")) {
//                try{
//                    //window cmd line 동작 method
//                    this.windowCmdLine(modelMap, args, cmd);
//                }catch (IOException | InterruptedException e) {
//                    modelMap.put("error", "명령어가 잘못되었습니다.");
//                }
//            }
//        }else {
//            //linux
//            cmd = new String[]{"/bin/sh", "-c", "cd " + this.cd + ";"};
//
//            if (args != null && !args.equals("")) {
//                try {
//                    if (args.startsWith("cd")) {
//                        String str = "";
//
//                        //cd를 사용 못하므로 cd 요청시 현재 pwd 값을 구해서 "cd ~~; args" 식으로 동작시킴
//                        String[] split = args.split(" ");
//                        if (split[1].equals("..")) {
//                            //PWD 명령어 실행 (현재 path 값 가져오기)
//                            this.cd = this.linuxCmdRunPWD(cmd, this.cd);
//                            cmd[2] = "cd " + this.cd + ";";
//                        } else if (split[1].startsWith("/")) {
//                            args = args.replace("cd ", "");
//                            this.cd = args;
//                            cmd[2] = "cd " + args + ";";
//                        } else {
//                            String[] cdSplit = this.cd.trim().replace("..", "").split("/");
//                            String tail = "";
//                            str = "";
//
//                            if(this.cd.contains("..")){
//                                cdSplit[cdSplit.length-1] = args.replace("cd ", "");
//                            }else {
//                                tail = args.replace("cd ", "");
//                            }
//
//                            for(int i=1; i<cdSplit.length; i++){
//                                str += "/" + cdSplit[i];
//                            }
//
//                            if (tail != null) {
//                                str += "/" + tail;
//                            }
//
//                            this.cd = str;
//                            cmd[2] = "cd " + this.cd + ";";
//                        }
//                    } else {
//                        cmd[2] = "cd " + this.cd + "; " + args;
//                    }
//
//                    this.linuxCmdLine(modelMap, cmd);
//                } catch (IOException | InterruptedException e) {
//                    modelMap.put("error", "명령어가 잘못되었습니다.");
//                }
//            }
//        }
//
//        return "test";
//    }
}
