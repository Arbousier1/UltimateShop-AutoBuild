import re
import sys

def smart_merge_pom(our_file, upstream_file, output_file):
    with open(our_file, 'r', encoding='utf-8') as f:
        our_content = f.read()
    with open(upstream_file, 'r', encoding='utf-8') as f:
        upstream_content = f.read()

    critical_fixes = {
        (r'<artifactId>flatten-maven-plugin</artifactId>\s*<version>1\.3\.0</version>',
         '<artifactId>flatten-maven-plugin</artifactId>\n                <version>1.6.0</version>'):
        "Upgrade flatten plugin for Maven 3.9.6 compatibility",
        (r'(</flatten-maven-plugin>\s*</plugin>)',
         r'''\1
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.codehaus.plexus</groupId>
                        <artifactId>plexus-archiver</artifactId>
                        <version>4.4.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.codehaus.plexus</groupId>
                        <artifactId>plexus-io</artifactId>
                        <version>3.2.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.codehaus.plexus</groupId>
                        <artifactId>plexus-interpolation</artifactId>
                        <version>1.27</version>
                    </dependency>
                    <dependency>
                        <groupId>commons-io</groupId>
                        <artifactId>commons-io</artifactId>
                        <version>2.8.0</version>
                    </dependency>
                    <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-compress</artifactId>
                        <version>1.20</version>
                    </dependency>
                </dependencies>
            </plugin>'''):
        "Add jar plugin configuration with required dependencies"
    }

    merged_content = our_content

    upstream_repos = re.search(r'<repositories>(.*?)</repositories>', upstream_content, re.DOTALL)
    our_repos = re.search(r'<repositories>(.*?)</repositories>', our_content, re.DOTALL)

    if upstream_repos and our_repos:
        upstream_repo_list = re.findall(r'<repository>.*?</repository>', upstream_repos.group(1), re.DOTALL)
        our_repo_list = re.findall(r'<repository>.*?</repository>', our_repos.group(1), re.DOTALL)
        our_repo_ids = set()
        for repo in our_repo_list:
            match = re.search(r'<id>(.*?)</id>', repo)
            if match:
                our_repo_ids.add(match.group(1))
        new_repos = []
        for repo in upstream_repo_list:
            match = re.search(r'<id>(.*?)</id>', repo)
            if match and match.group(1) not in our_repo_ids:
                new_repos.append(repo)
        if new_repos:
            print(f"Adding {len(new_repos)} new repositories from upstream")
            new_repos_str = '\n        '.join(new_repos)
            merged_content = re.sub(
                r'(</repositories>)',
                f'        {new_repos_str}\n    </repositories>',
                merged_content
            )

    for pattern, replacement in critical_fixes.items():
        if isinstance(pattern, tuple):
            merged_content = re.sub(pattern[0], replacement, merged_content)
        else:
            merged_content = re.sub(pattern, replacement, merged_content)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(merged_content)
    print("Smart merge completed")
    return True

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: python smart_merge_pom.py <our_pom> <upstream_pom> <output_pom>")
        sys.exit(1)
    success = smart_merge_pom(sys.argv[1], sys.argv[2], sys.argv[3])
    sys.exit(0 if success else 1)
