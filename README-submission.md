# SSH Log Analysis with BTrees
### Team Name
[Team Name TBD]

## Team Members

| Last Name | First Name | GitHub User Name |
|-----------|------------|------------------|
| Korber       Devyn        DevynKore       |
| Brooks    | Rhys       | RhysBrooks1      |
| Stringham | James      | jwstringham      |

# Test Results
How many of the dumpfiles matched (using the check-dump-files.sh script)?
- The `user-ip` Is the only to not match the expected format.
- 8/9 all others passs check dump files script. 

How many of the btree query files results matched (using the check-btree-search.sh script)?
- Testing is in progress.
- James' perview
- currently unfinished 

How many of the database query files results matched (using the check-db-search.sh script)?
- 5/8 are passing

# AWS Notes
Brief reflection on your experience with running your code on AWS.
- It was easy because we had already done it before. No issues that were outside the local env.

# Reflection

## Technical Challenges and Solutions

Throughout this project, we encountered several technical challenges that required careful problem-solving:

1. **Extracting Correct Key Formats for Different Tree Types**: 
   - The most significant challenge was correctly extracting and formatting the keys for different tree types, especially the `user-ip` format. The solution required careful pattern matching and string extraction from SSH log entries.
   
2. **Handling Special Patterns in `user-ip` Keys**:
   - The `user-ip` format needed special handling to preserve patterns like `*****-IP` for failed password attempts, `.x.y-IP` for reverse mapping entries, and various numeric username patterns.
   - The solution involved identifying specific log patterns and extracting the appropriate username or prefix formats.

3. **Calculating Frequencies**:
   - Implementing frequency counting for keys required a flexible approach since the underlying `TreeObject` class might handle frequencies differently.
   - We used reflection to try multiple approaches for setting frequencies (via `setFrequency` or `setCount` methods).

4. **Maintaining Consistent Naming Conventions**:
   - The project required using a fixed degree value of 0 for filenames regardless of the actual BTree degree.
   - This separation between the computational degree and the filename degree was important for consistency.

## Learning Outcomes

Working on this project provided valuable insights into:

1. **BTree Data Structures**: Understanding how BTrees work for efficient storage and retrieval of data.

2. **Pattern Matching and Log Analysis**: Developing skills for extracting meaningful information from log files.

3. **Database Integration**: Learning how to store BTree data in SQLite databases for further analysis.

4. **Debugging and Testing**: The importance of comparing output against reference files to ensure correctness.

## Individual Reflections
To be completed by each team member.

## Reflection (Team member name: )
Devyn Korber
Implementing the disk‑backed B‑Tree for SSH log analysis pushed me to turn textbook algorithms into production‑quality code—handling fixed‑size node serialization, RandomAccessFile I/O, and optimal degree tuning for 4 KB blocks—while integrating an in‑memory cache to balance performance and resource limits. Building the top‑k frequency extractor with Java’s priority queue and mapping inorder traversals into SQLite tables sharpened my data‑structure and database skills, and writing end‑to‑end shell scripts for automated testing reinforced the value of reproducible workflows. Coordinating via GitHub issues, pull requests, and a Scrum board taught me the discipline of collaborative engineering, and ultimately transformed my understanding of how to design, implement, and validate a scalable pipeline from raw logs to searchable analytics in a real‑world setting.

## Reflection (Team member name: Rhys Brooks )
Implementing the Btree class helped me put into practice a lot of the fundamentals of working with trees and caches. It was also my first time working with reading and writing static memory using the disk write and read methods. Workign as a team asllowed me to understand some of the difficulties that come with asynchronous work as there were sometimes issues that appeared on one computer that did not appear on others. Overall I feel that this project sharpened my skills as both a programmer and a team player in the world of software development.

## Reflection (Team member name: James Stringham)
Working on the SSHSearchBTree module gave me a good experience in parsing logs and building a disk BTree search tool. My main focus was implementing the logic to read keys from query files, search them in the BTree, and return matching results. During testing, I encountered a quite a bit of errors including, NullPointerException errors when the search result was null. These were often caused by truncated or empty BTree files, which affected parsing. In other cases, small mismatches in key formats between the logs and query inputs led to failed lookups.

# Additional Notes
The most challenging aspect of this project was understanding the exact format required for the `user-ip` dump file. The file needed to preserve various patterns from the SSH log entries rather than normalizing them to a standard format. 

The solution involved carefully extracting usernames and IP addresses from different log patterns and maintaining the exact format expected by the check scripts. This demonstrates the importance of understanding requirements fully before implementation.
