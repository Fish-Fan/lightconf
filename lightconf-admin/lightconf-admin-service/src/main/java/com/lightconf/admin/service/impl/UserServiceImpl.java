package com.lightconf.admin.service.impl;

import com.lightconf.admin.dal.dao.UserMapper;
import com.lightconf.admin.model.dataobj.User;
import com.lightconf.admin.model.dataobj.UserExample;
import com.lightconf.admin.service.UserService;
import com.lightconf.common.model.Messages;
import com.lightconf.common.util.LightConfResult;
import com.lightconf.common.util.ResultCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Map;

/**
 * @author wuhf
 * @date 2018/04/21
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;

    @Override
    public LightConfResult userLogin(String userName, String password) {
        UserExample userExample = new UserExample();
        userExample.createCriteria().andUserNameEqualTo(userName);
        List<User> userList = userMapper.selectByExample(userExample);

        if (userList != null && userList.size() > 0) {
            User user = userList.get(0);
            if (user.getPassword().equals(password)) {
                return LightConfResult.build(Messages.SUCCESS_CODE,Messages.SUCCESS_MSG,userList.get(0));
            }
        }

        return LightConfResult.build(Messages.USER_LOGIN_ERROR_CODE,Messages.USER_LOGIN_ERROR_MSG);
    }

    @Override
    public Map<String, Object> getUserList(int start, int length, String username, int permission) {
        return null;
    }

    @Override
    public ResultCode<User> addUser(User confUser) {
        // valid
        if (StringUtils.isBlank(confUser.getUserName())){
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名不可为空");
        }
        if (StringUtils.isBlank(confUser.getPassword())){
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        if (!(confUser.getPassword().length()>=4 && confUser.getPassword().length()<=100)) {
//            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
        }

        // passowrd md5
        String md5Password = DigestUtils.md5DigestAsHex(confUser.getPassword().getBytes());
        confUser.setPassword(md5Password);
        return null;
    }

    @Override
    public ResultCode<User> deleteUser(String username) {
        return null;
    }

    @Override
    public ResultCode<User> updateUser(User confUser) {
        return null;
    }
}
