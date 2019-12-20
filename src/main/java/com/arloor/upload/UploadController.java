package com.arloor.upload;

import com.arloor.upload.aop.Metric;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Controller
@CommonsLog
public class UploadController {
    String userHome = System.getProperty("user.home");
    String parentDirPath = String.format("%s/upload", userHome);
    File parentDir = new File(parentDirPath);

    private static String[] inline_display_types={
            ".txt",".sh",".json",".log",
            ".png",".jpg",".jpeg",
            ".pdf",
            ".mp4"
    };

    {
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private List<FileVo> children(File dir) {
        File[] files = dir.listFiles();
        return Arrays.stream(files)
                .map(cell -> {
                    String path=parentDir.toPath()
                            .relativize(cell.toPath())
                            .toString();
                    FileVo fileVo=FileVo.builder()
                            .name((cell.isDirectory()?"___"+cell.getName()+"___":cell.getName()))
                            .url("/"+path)
                            .build();
                    if(cell.isFile()){
                        fileVo.setUrl("/view/"+urlEncode(path));
                    }
                    return fileVo;
                })
                .collect(Collectors.toList());
    }


    public static final String urlEncode(String path){
        path=path.replace("\\","/");
        String[] pathCells=path.split("/");
        String encodedPath=Arrays.stream(pathCells).map(pathCell-> {
            try {
                return URLEncoder.encode(pathCell,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return pathCell;
        }).collect(Collectors.joining("/"));
        return encodedPath;
    }

    @GetMapping("/")
    @Metric
    public String index(Model model,HttpServletRequest request) {
        model.addAttribute("files", children(parentDir));
        return "upload";
    }



    private String handleList(Model model, String path) {
        try {
            path=URLDecoder.decode(path,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
        }else{//是文件，应该走不到的
            return "redirect:/view"+urlEncode(path);
        }
    }

    @GetMapping("/view/**")
    @ResponseBody
    @Metric
    public String down(HttpServletRequest request,HttpServletResponse response) throws UnsupportedEncodingException {
        String file=request.getRequestURI().replace("/view","");
        file= URLDecoder.decode(file,"UTF-8");

        File toDownload = new File(parentDirPath+file);
        //这种想超越预定文件夹，下载其他文件，禁止！
        if(file.contains("..")){
            return "非法路径";
        }
        // 如果文件存在，则进行下载
        if (toDownload.exists()) {
//            // 配置文件下载
//            response.setHeader("content-type", "application/octet-stream");
//            response.setContentType("application/octet-stream");
//

            AtomicBoolean inline = new AtomicBoolean(false);
            Arrays.stream(inline_display_types).forEach((inlineType)->{
                if(toDownload.getName().contains(inlineType)){
                    inline.set(true);
                    response.setHeader("Content-Disposition", "inline");
                }
            });

            if(!inline.get()){
                response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(toDownload.getName(), "UTF-8"));
            }

            // 实现文件下载
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try(ServletOutputStream outputStream=response.getOutputStream()) {
                long size = Files.copy(toDownload.toPath(),outputStream );
            } catch (IOException e) {
                e.printStackTrace();
                return "下载失败!";
            }
        }
        return "";
    }


    @PostMapping("/uploadfile")
    @Metric
    public String upload(HttpServletRequest request,Model model,@RequestParam("file") MultipartFile file,@RequestParam("url") String url) {
        try {
            url=URLDecoder.decode(url,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
            model.addAttribute("msg",msg);
        } catch (IOException e) {
            model.addAttribute("msg",e.toString());
        }
        model.addAttribute("url",url);
        model.addAttribute("placehold","返回"+url);
        return "upload-result";
    }

//    @GetMapping("/{path1}")
//    public String list(Model model, @PathVariable String path1) {
//        String path = buildPath(path1);
//        return handleList(model,path);
//    }
//
//    @GetMapping("/{path1}/{path2}")
//    public String list2(Model model, @PathVariable String path1, @PathVariable String path2) {
//        String path = buildPath(path1, path2);
//        return handleList(model,path);
//    }
//
//    @GetMapping("/{path1}/{path2}/{path3}")
//    public String list3(Model model, @PathVariable String path1, @PathVariable String path2,@PathVariable String path3) {
//        String path = buildPath(path1, path2, path3);
//        return handleList(model,path);
//    }
//
//    @GetMapping("/{path1}/{path2}/{path3}/{path4}")
//    public String list3(Model model, @PathVariable String path1, @PathVariable String path2,@PathVariable String path3,@PathVariable String path4) {
//        String path = buildPath(path1, path2, path3,path4);
//        return handleList(model,path);
//    }

    @GetMapping("/**")
    @Metric
    public String listsss(Model model,HttpServletRequest request){
        return handleList(model,request.getRequestURI());

    }

}
