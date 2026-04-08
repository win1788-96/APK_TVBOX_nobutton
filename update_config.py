import os
import re
import shutil

def update_config():
    print("=== Android Project Configuration Updater ===")
    
    # Base paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    gradle_file = os.path.join(base_dir, "app", "build.gradle.kts")
    src_base_dir = os.path.join(base_dir, "app", "src", "main", "java")
    
    # 1. Get user input
    new_package = input("請輸入新的 APP 包名 (例如 com.new.app): ").strip()
    new_url = input("請輸入新的 fixedUrl 網址: ").strip()
    
    if not new_package or not new_url:
        print("錯誤：輸入不能為空！")
        return

    # 2. Update build.gradle.kts
    if os.path.exists(gradle_file):
        with open(gradle_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Replace namespace and applicationId
        content = re.sub(r'namespace\s*=\s*".*?"', f'namespace = "{new_package}"', content)
        content = re.sub(r'applicationId\s*=\s*".*?"', f'applicationId = "{new_package}"', content)
        
        with open(gradle_file, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"已更新 {gradle_file}")
    else:
        print(f"錯誤：未找到 {gradle_file}")

    # 3. Find current package and source files
    # We'll look into the directories to find where the .kt files are
    old_package = None
    old_src_path = None
    
    for root, dirs, files in os.walk(src_base_dir):
        for file in files:
            if file.endswith(".kt"):
                with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                    line = f.readline()
                    match = re.match(r'^package\s+(.*)', line)
                    if match:
                        old_package = match.group(1).strip()
                        old_src_path = root
                        break
        if old_package:
            break

    if not old_package:
        print("錯誤：無法偵測到原始包名。")
        return

    print(f"偵測到原始包名: {old_package}")

    # 4. Update all .kt files and handle URL in MainActivity
    all_kt_files = []
    for root, dirs, files in os.walk(src_base_dir):
        for file in files:
            if file.endswith(".kt"):
                all_kt_files.append(os.path.join(root, file))

    for filepath in all_kt_files:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Update package declaration
        content = re.sub(r'^package\s+.*', f'package {new_package}', content, flags=re.MULTILINE)
        
        # Update fixedUrl if it's MainActivity
        if "MainActivity.kt" in filepath:
            content = re.sub(r'private val fixedUrl\s*=\s*".*?"', f'private val fixedUrl = "{new_url}"', content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"已更新程式碼: {os.path.basename(filepath)}")

    # 5. Move files to new directory structure
    new_relative_path = new_package.replace('.', os.sep)
    new_src_path = os.path.join(src_base_dir, new_relative_path)

    if old_src_path != new_src_path:
        print(f"正在移動目錄從 {old_src_path} 到 {new_src_path}")
        os.makedirs(new_src_path, exist_ok=True)
        
        # Move all files from old path to new path
        for file in os.listdir(old_src_path):
            old_file_path = os.path.join(old_src_path, file)
            new_file_path = os.path.join(new_src_path, file)
            if os.path.isfile(old_file_path):
                shutil.move(old_file_path, new_file_path)
        
        # Optionally clean up old empty directories (this is a bit complex to do safely, 
        # so we'll just leave them for now or move the whole folder if possible)
        print("目錄移動完成。")

    print("\n修改成功！您可以開始編譯新的版本了。")

if __name__ == "__main__":
    update_config()
