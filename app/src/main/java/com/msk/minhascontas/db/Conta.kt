package com.msk.minhascontas.db

/*
Conta.kt

O que é
- Entity Kotlin (Room) que mapeia a tabela SQLite "contas" existente no app antigo.
- Projetada para manter compatibilidade com o schema gerado por DBContas.java:
  as anotações @ColumnInfo usam os mesmos nomes de coluna que o esquema original,
  permitindo que Room leia/escreva no mesmo arquivo de banco de dados sem
  necessidade de reescrever dados.

Campos/Colunas
- id (_id): primary key autogerada (Long).
- nome (conta): nome da conta/lançamento (String).
- tipoConta (tipo_conta): inteiro representando tipo (receita/despesa/aplicação). Pode ser null se o registro antigo tiver inconsistência.
- classeConta (classe_conta): inteiro representando a classe dentro do tipo.
- categoriaConta (categoria_conta): originalmente definido como TEXT no schema antigo. Mantive como String para garantir leitura segura de dados antigos que podem ter valores textuais; se preferir usar Int, implementar TypeConverter/migration.
- dia (dia_data), mes (mes_data), ano (ano_data): componentes de data como Int.
- valor (valor): valor monetário (Double).
- pagou (pagou): string representando estado de pagamento (ex.: "paguei", "falta").
- qtRepeticoes (qt_repeticoes), nrRepeticao (nr_repeticao), intervalo (dia_repeticao): campos de repetição.
- codigo (codigo): string identificadora de série.

Como usar
- Instanciar para inserção:
    val conta = Conta(
        nome = "Luz",
        tipoConta = 0,
        classeConta = 2,
        categoriaConta = "7",
        dia = 10, mes = 5, ano = 2025,
        valor = 123.45,
        pagou = "falta",
        qtRepeticoes = 12,
        nrRepeticao = 1,
        intervalo = 30,
        codigo = "abc-123"
    )
- Inserir via DAO:
    val id = contaDao.insert(conta)
- Observações:
  - Os nomes das colunas foram preservados propositalmente para compatibilidade.
  - Evite mudar o nome de qualquer @ColumnInfo sem criar migração apropriada.
  - Recomenda-se migrar para tipos Kotlin/fortes (ex.: usar enums para pagou ou converter categoria para Int)
    com migration explícita para evitar perda/erro ao abrir DB legado.
*/

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contas")
data class Conta(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    val id: Long = 0L,

    @ColumnInfo(name = "conta")
    val nome: String,

    @ColumnInfo(name = "tipo_conta")
    val tipoConta: Int?,

    @ColumnInfo(name = "classe_conta")
    val classeConta: Int?,

    // original schema declares categoria as TEXT; keep it as String to avoid read issues
    @ColumnInfo(name = "categoria_conta")
    val categoriaConta: String?,

    @ColumnInfo(name = "dia_data")
    val dia: Int,

    @ColumnInfo(name = "mes_data")
    val mes: Int,

    @ColumnInfo(name = "ano_data")
    val ano: Int,

    @ColumnInfo(name = "valor")
    val valor: Double,

    @ColumnInfo(name = "pagou")
    val pagou: String?,

    @ColumnInfo(name = "qt_repeticoes")
    val qtRepeticoes: Int,

    @ColumnInfo(name = "nr_repeticao")
    val nrRepeticao: Int,

    @ColumnInfo(name = "dia_repeticao")
    val intervalo: Int,

    @ColumnInfo(name = "codigo")
    val codigo: String?
)
