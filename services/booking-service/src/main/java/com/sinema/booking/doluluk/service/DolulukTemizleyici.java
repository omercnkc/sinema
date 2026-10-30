package com.sinema.booking.doluluk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Doluluğu değiştiren işlemler (bırakma, onay, ret) bitince seansın önbellek kaydını siler (K47).
 *
 * Transaction içindeyse silme commit'ten sonra yapılır: önce silinseydi, arada gelen bir doluluk isteği henüz
 * commit edilmemiş eski durumu okuyup önbellek süresi boyunca saklayabilirdi. Geri alınan işlemde silinmez
 * (doluluk değişmedi). Transaction yoksa hemen silinir.
 */
@Component
@RequiredArgsConstructor
public class DolulukTemizleyici {

    private final DolulukOnbellegi onbellek;

    public void degisti(Long seansId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            onbellek.sil(seansId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                onbellek.sil(seansId);
            }
        });
    }
}
