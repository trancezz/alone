package com.twotrance.alone.service.segment;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.twotrance.alone.model.segment.Paragraph;
import com.twotrance.alone.repositorys.ParagraphRepository;
import com.twotrance.alone.service.common.BaseAbstractService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * ParagraphService
 *
 * @author trance
 * @description paragraph service
 */
@Service
public class ParagraphService extends BaseAbstractService {

    @Resource
    private ParagraphRepository paragraphRepository;

    public Set<String> models() {
        return Optional.ofNullable(paragraphRepository.models()).orElse(CollUtil.empty(HashSet.class));
    }

    public void updateOfMax(Long newMax, String model, Long oldMax) {
        paragraphRepository.updateOfMax(newMax, model, oldMax);
    }

    public Paragraph addModel(Paragraph paragraph) {
        if (!validAppKey(paragraph.getPhone(), paragraph.getAppKey()).getAdmin().booleanValue())
            throwException(1003);
        Paragraph exParagraph = paragraphRepository.findByPhoneAndModel(paragraph.getPhone(), paragraph.getModel());
        if (ObjectUtil.isNotEmpty(exParagraph))
            throwException(3005);
        Paragraph saveParagraph = null;
        try {
            saveParagraph = paragraphRepository.save(paragraph);
        } catch (Exception e) {
            throwException(1001);
        }
        return saveParagraph;
    }

    public Paragraph model(String phone, String model) {
        return paragraphRepository.findByPhoneAndModel(phone, model);
    }

    public boolean hasModel(String phone, String model) {
        return ObjectUtil.isNotEmpty(paragraphRepository.findByPhoneAndModel(phone, model)) ? true : false;
    }

}
