package org.example.examsystem.service.Impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.examsystem.entity.TesterExam;
import org.example.examsystem.mapper.TesterExamMapper;

import org.example.examsystem.service.IService.IGradeService;
import org.example.examsystem.vo.GradeInfoVO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class   GradeServiceImpl extends ServiceImpl<TesterExamMapper, TesterExam> implements IGradeService {


    private final TesterExamMapper testerExamMapper;

    /**
     * 查询某次考试成绩列表情况
     * @param examId 考试Id
     * @return GradeInfoVO 分页
     */
    @Override
    public Page<GradeInfoVO> getGrades(Long examId, Integer page, Integer pageSize) {
        Page<GradeInfoVO> pageInfo=new Page<>(page,pageSize);
        return testerExamMapper.getGrades(pageInfo,examId);
    }
}
