package com.nubixconta.modules.accounting.entity;

import com.nubixconta.modules.administration.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

// ================================================================
// --- CLASE #1: La Entidad Principal ---
// Esta es la única clase PÚBLICA (no estática) del archivo.
// ================================================================
@Entity
@Table(name = "accounting_setting")
@Getter
@Setter
@NoArgsConstructor
public class AccountingSetting {

    // --- ¡CLASE INTERNA, ESTÁTICA Y PÚBLICA PARA EL ID! ---
    // 'public' la hace visible fuera de este archivo.
    // 'static' significa que no depende de una instancia de AccountingSetting.
    // Es la forma correcta de definir una clase de ID interna.
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class AccountingSettingId implements Serializable {

        @Column(name = "setting_key", length = 50)
        private String settingKey;

        @Column(name = "company_id")
        private Integer companyId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AccountingSettingId that = (AccountingSettingId) o;
            return Objects.equals(settingKey, that.settingKey) && Objects.equals(companyId, that.companyId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(settingKey, companyId);
        }
    }

    // --- El resto de la entidad principal ---

    @EmbeddedId
    private AccountingSettingId id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_catalog", nullable = false)
    private Catalog catalog;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("companyId")
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountingSetting that = (AccountingSetting) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}