package com.arloor.upload;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Controller
@CommonsLog
public class UploadController {
    String userHome = System.getProperty("user.home");
    String parentDirPath = String.format("%s/upload", userHome);
    File parentDir = new File(parentDirPath);

    {
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private List<FileVo> children(File dir) {
        File[] files = dir.listFiles();
        return Arrays.stream(files)
                .map(cell -> {
                    String path="/"+parentDir.toPath()
                            .relativize(cell.toPath())
                            .toString();
                    FileVo fileVo=FileVo.builder()
                            .name((cell.isDirectory()?"___"+cell.getName()+"___":cell.getName()))
                            .url(path)
                            .build();
                    if(cell.isFile()){
                        try {
                            fileVo.setUrl("/download?file="+URLEncoder.encode(path,"UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    return fileVo;
                })
                .collect(Collectors.toList());
    }


    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("files", children(parentDir));
        return "upload";
    }



    private String handleList(Model model, String path) {
        String filePath = String.format("%s%s", parentDirPath, path);
        File file = new File(filePath);
        if(!file.exists()){//路径不存在则报错
            return "error";
        }
        if (file.isDirectory()) {//是文件夹，列出所有文件
            List<FileVo> files=children(file);
            files.add(0,FileVo.builder().name("___上级文件夹___").url("/"+parentDir.toPath()
                    .relativize(file.getParentFile().toPath())
                    .toString()).build());
            model.addAttribute("files",files );
            return "upload";
        }else{//是文件，下载
            try {
                return "redirect:/download?file="+URLEncoder.encode(path,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return "/error";
            }
        }
    }

    @GetMapping("/{path1}")
    public String list(Model model, @PathVariable String path1) {
        String path = buildPath(path1);
        return handleList(model,path);
    }



//   http://localhost:8080/download?file=%2F..%2F.ssh%2Fid_rsa
    @GetMapping("/download")
    @ResponseBody
    public String down(String file,HttpServletResponse response) throws UnsupportedEncodingException {
        log.info("下载路径："+file);

        File toDownload = new File(parentDirPath+file);
        //这种想超越预定文件夹，下载其他文件，禁止！
        if(file.contains("..")){
            return "非法路径";
        }
        // 如果文件存在，则进行下载
        if (toDownload.exists()) {
            // 配置文件下载
            response.setHeader("content-type", "application/octet-stream");
            response.setContentType("application/octet-stream");
            // 下载文件能正常显示中文
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(toDownload.getName(), "UTF-8"));
            // 实现文件下载
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try(ServletOutputStream outputStream=response.getOutputStream()) {
                long size = Files.copy(toDownload.toPath(),outputStream );
                log.info(size);
            } catch (IOException e) {
                e.printStackTrace();
                return "fail!";
            }
        }
        return "";
    }
    private String buildPath(String... paths) {
        StringBuffer sb = new StringBuffer();
        for (String path : paths
                ) {
            sb.append("/");
            sb.append(path);
        }
        log.info("路径组合为："+sb.toString());
        return sb.toString();
    }


    @PostMapping("/uploadfile")
    public String upload(Model model,@RequestParam("file") MultipartFile file,@RequestParam("url") String url) {
        if (file.isEmpty()) {
            return "上传失败，请选择文件";
        }
        //这种想超越预定文件夹，覆盖其他文件，禁止！
        if(url.contains("..")){
            return "error";
        }

        String fileName = file.getOriginalFilename();
        String filePath = parentDirPath +url+"/"+ fileName;
        File target = new File(filePath);
        //如果存在则跟随时间戳
        if (target.exists()){
            filePath+= "_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            target = new File(filePath);
        }
        String absolutePath = target.getAbsolutePath();
        try(InputStream inputStream=file.getInputStream()) {
            //复制文件，如果存在则覆盖
            Files.copy(inputStream, Paths.get(absolutePath));
            String msg = String.format("success: to %s", absolutePath);
            log.info(msg);
            model.addAttribute("msg",msg);
        } catch (IOException e) {
            log.error(e.toString(), e);
            model.addAttribute("msg",e.toString());
        }
        model.addAttribute("url",url);
        model.addAttribute("placehold","返回"+url);
        return "upload-result";
    }

    @GetMapping("/{path1}/{path2}")
    public String list2(Model model, @PathVariable String path1, @PathVariable String path2) {
        String path = buildPath(path1, path2);
        return handleList(model,path);
    }

    @GetMapping("/{path1}/{path2}/{path3}")
    public String list3(Model model, @PathVariable String path1, @PathVariable String path2,@PathVariable String path3) {
        String path = buildPath(path1, path2, path3);
        return handleList(model,path);
    }

    @GetMapping("/{path1}/{path2}/{path3}/{path4}")
    public String list3(Model model, @PathVariable String path1, @PathVariable String path2,@PathVariable String path3,@PathVariable String path4) {
        String path = buildPath(path1, path2, path3,path4);
        return handleList(model,path);
    }

}
