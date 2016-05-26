package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Marco on 2016-05-18.
 */

@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);
    private Map<Long,Video> videos = new HashMap<Long, Video>();
    private VideoFileManager myManager;

    public Video save(Video entity) {
        checkAndSetId(entity);
        entity.setDataUrl(getDataUrl(entity.getId()));
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }

    //GET /video
    @RequestMapping(value = "/video", method = RequestMethod.GET)
    public @ResponseBody
    Collection<Video> getVideo()
    {
        return videos.values();
    }

    //POST /video
    @RequestMapping(value ="/video", method=RequestMethod.POST)
    public @ResponseBody Video postVideo(@RequestBody Video v)
    {
        Video toReturn = save(v);
        return toReturn;
    }

    //POST /video/{id}/data
    @RequestMapping(value = "/video/{id}/data", method=RequestMethod.POST)
    public @ResponseBody VideoStatus postVideoID(@PathVariable("id") Long id, @RequestParam("data") MultipartFile dataFile, HttpServletResponse response)
    {
        if(!videos.containsKey(id))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        try {
            myManager = VideoFileManager.get();
            saveSomeVideo(videos.get(id), dataFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        VideoStatus toReturn = new VideoStatus(VideoStatus.VideoState.READY);
        return toReturn;

    }

    //GET /video/{id}/data
    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
    public void getVideoData(@PathVariable("id") Long videoID, HttpServletResponse response)
    {
        if(!videos.containsKey(videoID))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if(!myManager.hasVideoData(videos.get(videoID)))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        serveSomeVideo(videos.get(videoID), response);

    }


    public void serveSomeVideo(Video v, HttpServletResponse response)
    {
        try {
            myManager.copyVideoData(v, response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String base =
                "http://"+request.getServerName()
                        + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
        return base;
    }

    public void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
        myManager.saveVideoData(v, videoData.getInputStream());
    }
}
