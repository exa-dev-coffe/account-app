package com.time_tracker.be.tokenResetPassword;

import com.time_tracker.be.account.AccountModel;
import com.time_tracker.be.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ResetTokenPasswordService {
    private final ResetTokenPasswordRepository resetTokenPasswordRepository;

    public ResetTokenPasswordService(ResetTokenPasswordRepository resetTokenPasswordRepository) {
        this.resetTokenPasswordRepository = resetTokenPasswordRepository;
    }

    public void addResetToken(String resetToken, AccountModel accountModel) {
        ResetTokenPasswordModel resetTokenPasswordModel = new ResetTokenPasswordModel();
        resetTokenPasswordModel.setToken(resetToken);
        resetTokenPasswordModel.setUserId(accountModel);
        resetTokenPasswordModel.setExpiryAt(new java.util.Date(System.currentTimeMillis() + 5 * 60 * 1000)); // 5 minutes
        resetTokenPasswordRepository.save(resetTokenPasswordModel);
    }

    public void updateResetToken(String resetToken, AccountModel accountModel) {
        ResetTokenPasswordModel existingToken = resetTokenPasswordRepository.findByToken(resetToken);
        if (existingToken != null && !existingToken.isUsed()) {
            existingToken.setUsed(true);
            resetTokenPasswordRepository.save(existingToken);
        } else {
            throw new BadRequestException("Reset Token does not exist or has already been used");
        }
    }

    public boolean checkWasLimitOneDay(AccountModel accountModel) {
        Calendar calStart = Calendar.getInstance();
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);
        calStart.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calStart.getTime();

        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calEnd.getTime();

        List<ResetTokenPasswordModel> existingTokens = resetTokenPasswordRepository
                .findByUserIdAndCreatedAtBetween(accountModel, startOfDay, endOfDay);

        return existingTokens != null && existingTokens.size() >= 3;
    }

}
