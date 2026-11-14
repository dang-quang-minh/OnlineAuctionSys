package com.auction.app.server.repository;

import java.io.IOException;
import java.io.Serializable;

/**
 * Giao diện định nghĩa các thao tác lưu trữ và tải dữ liệu bền vững.
 */
public interface StorageRepository {

  /**
   * Lưu một đối tượng Serializable xuống đường dẫn file chỉ định.
   *
   * @param filePath Đường dẫn tệp lưu trữ
   * @param data     Đối tượng dữ liệu cần lưu
   * @param <T>      Kiểu dữ liệu thực thi Serializable
   * @throws IOException Lỗi trong quá trình ghi luồng I/O
   */
  <T extends Serializable> void save(String filePath, T data) throws IOException;

  /**
   * Tải dữ liệu đối tượng từ đường dẫn file chỉ định.
   *
   * @param filePath Đường dẫn tệp dữ liệu
   * @param <T>      Kiểu dữ liệu mong muốn trả về
   * @return Đối tượng đã được đọc từ file, hoặc null nếu file chưa tồn tại
   * @throws IOException            Lỗi trong quá trình đọc luồng I/O
   * @throws ClassNotFoundException Không tìm thấy lớp tương ứng khi giải tuần tự hóa
   */
  <T extends Serializable> T load(String filePath) throws IOException, ClassNotFoundException;
}
