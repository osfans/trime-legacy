同文安卓輸入法平臺/Trime
=====
源於開源的[注音倉頡輸入法]，
參考著名的[Rime]輸入法，
藉助數據庫與正則表達式，
使用安卓的java語言書寫，
旨在保護漢語各地方言，
音碼形碼通用輸入法平臺。

## 歷史
- 最初，TRIME輸入法是寫給[泰如拼音]（thae5 rv2）的，中文名爲“泰如輸入法”。
- 後來，添加了吳語等方言碼表，做成了一個輸入法平臺，更名爲“漢字方言輸入法”。
- 最近，兼容了五筆、兩筆等形碼，在太空衛士、徵羽的建議下，更名爲“同文輸入法平臺”。寓意音碼形碼同臺，方言官話同文。
- 所以，你可以認爲TRIME是Tongwen RIME或是ThaeRvInputMEthod的縮寫。

## 針對方言
- 反查字音
- 輸出拼音
- 模糊音
- 國際音標同音字表
- 支持unicode ext-A/B/C/D大字庫方言正字

## 使用自定義方案
- 參考[碼表格式說明](https://github.com/osfans/trime-tool/blob/master/data/README.md)，編寫自定義方案
- 使用[trime-tool]生成trime.db，放到assests目錄下，重新編譯生成新apk。
- 從設置中導入trime.db

## 已有功能
- 自定義方案：拼音、注音、音標等。長按漢字鍵，切換方案。短按漢字鍵，切換中英文。
- 自定義鍵盤：全拼、雙拼、字母、音標、漢字等。短按拼音鍵，切換鍵盤。
- 模糊音：尖團、平翹、前後鼻音等。長按拼音鍵，查看方案信息，設置模糊音。
- 詞語輸入：支持簡拼，自動音節切分。短按逗號或'，手動切分音節。
- 詞語聯想：自動根據最後一個字進行聯想。
- 輸出簡體：啓用時，候選字爲繁體，輸出時使用[opencc]轉換爲簡體。
- 輸出拼音：啓用時，自動在輸出的漢字後加括號和注音。
- 反查字音：長按逗號，查詢光標處漢字讀音。

## [將有功能](https://github.com/osfans/trime/issues?q=is%3Aopen+is%3Aissue+label%3A%E5%8A%9F%E8%83%BD)


[trime-tool]: https://github.com/osfans/trime-tool/
[opencc]: https://github.com/BYVoid/OpenCC
[Rime]: https://code.google.com/p/rimeime/
[注音倉頡輸入法]: https://code.google.com/p/android-traditional-chinese-ime/
[泰如拼音]: http://tieba.baidu.com/f?kw=%E6%B3%B0%E5%A6%82
